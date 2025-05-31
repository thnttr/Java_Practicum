package server;

import server.handler.ClientHandler;
import server.models.MainAction.Action;
import server.models.Message;
import server.models.UserSession;
import server.util.DatabaseUtil;
import server.util.DraftManager;
import server.util.UserManager;

import java.io.*;
import java.nio.file.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerMain {
    private static Set<ClientHandler> onlineUsers = Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean running = true;
    private static int port;
    private static String address;
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        initialize();
        startCommandListener();
        startServer();
    }

    private static void initialize() {
        System.out.println("Server Initializing...");
        try {
            // 1. 初始化数据库
            DatabaseUtil.initialize();
            
            // 2. 初始化目录
            initializeDirectories();
            
            // 3. 加载配置
            loadConfig();
            
            // 4. 加载草图历史
            DraftManager.loadDraftHistory();
            
            System.out.println("Server initialization completed");
        } catch (Exception e) {
            System.err.println("Server initialization failed: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void initializeDirectories() {
        try {
            // 获取项目根目录
            String projectRoot = new File("").getAbsolutePath();
            
            // 检查并创建local_draft文件
            Path draftPath = Paths.get(projectRoot, "local_draft.ser");
            if (!Files.exists(draftPath)) {
                Files.createFile(draftPath);
                System.out.println("Created local_draft.ser file");
            }
            
        } catch (IOException e) {
            System.err.println("Failed to initialize directories: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void loadConfig() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("src/main/resources/server-config.properties")) {
            props.load(input);
            port = Integer.parseInt(props.getProperty("server.port", "8888"));
            address = props.getProperty("server.address", "localhost");
            System.out.println("Server address: " + address + ":" + port);
        } catch (IOException e) {
            System.err.println("Failed to load config: " + e.getMessage());
            System.exit(1);
        }
    }

    private static void startCommandListener() {
        Thread commandThread = new Thread(() -> {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(System.in))) {
                while (running) {
                    String command = reader.readLine();
                    if ("stop server".equalsIgnoreCase(command)) {
                        if (!DraftManager.isBeingEdited()) {
                            shutdown();
                            break;
                        } else {
                            System.out.println("Cannot stop server while client is editing");
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        commandThread.setDaemon(true);
        commandThread.start();
    }

    private static void startServer() {
        try {
            serverSocket = new ServerSocket(port);
            serverSocket.setSoTimeout(1000); // 设置accept超时为1秒
            System.out.println("Server ready. Waiting for connections...");
            ExecutorService executor = Executors.newCachedThreadPool();
            
            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    ClientHandler handler = new ClientHandler(socket);
                    executor.execute(handler);
                } catch (SocketTimeoutException e) {
                    // 忽略超时异常，继续检查running状态
                    continue;
                }
            }
        } catch (IOException e) {
            if (running) {
                System.err.println("Server error: " + e.getMessage());
            }
        } finally {
            if (serverSocket != null && !serverSocket.isClosed()) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void shutdown() {
        running = false;
        // 广播退出消息
        broadcastMessage(new Message(Message.Type.EXIT, "Server is shutting down", null));
        
        // 等待消息发送完成
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // 关闭所有客户端连接
        for (ClientHandler client : new HashSet<>(onlineUsers)) {
            client.closeConnection();
        }
        
        // 保存当前状态
        DraftManager.saveDraftHistory();
        
        // 关闭数据库连接池
        DatabaseUtil.shutdown();
        
        System.out.println("Server shutdown completed");
        System.exit(0);
    }

    public static synchronized void handleClientRegistration(ClientHandler client, 
            UserSession userSession) {
        String ipAddress = client.getSocket().getInetAddress().getHostAddress();
        UserSession existingUser = UserManager.getUserByStudentId(userSession.getStudentId());
        
        if (existingUser == null) {
            UserManager.addUser(userSession.getUsername(), 
                              userSession.getStudentId(), 
                              ipAddress);
        } else {
            UserManager.updateUser(userSession.getStudentId(), ipAddress);
        }

        onlineUsers.add(client);
        broadcastUserUpdate();
    }


    public static synchronized boolean requestEditPermission(ClientHandler client) {
        if (!DraftManager.isBeingEdited()) {
            DraftManager.setBeingEdited(true);
            DraftManager.setCurrentEditor(client.getUserSession().getUsername());
            return true;
        }
        return false;
    }

    public static synchronized void handleEditComplete() {
        DraftManager.commitEdit();
        broadcastDraftUpdate();
        DraftManager.setBeingEdited(false);
        DraftManager.setCurrentEditor(null);
    }

    public static synchronized void handleClientExit(ClientHandler client) {
        onlineUsers.remove(client);
        if (client.getUserSession().getUsername()
                .equals(DraftManager.getCurrentEditor())) {
            DraftManager.setBeingEdited(false);
            DraftManager.setCurrentEditor(null);
        }
        broadcastUserUpdate();
    }

    private static void broadcastUserUpdate() {
        Set<UserSession> users = new HashSet<>();
        for (ClientHandler client : onlineUsers) {
            users.add(client.getUserSession());
        }
        broadcastMessage(new Message(Message.Type.UPDATE_USERS, users, null));
    }

    public static synchronized void broadcastEditAction(Action action, ClientHandler sender) {
        Message message = new Message(Message.Type.EDIT_ACTION, action, 
                sender.getUserSession().getUsername());
        broadcastToOthers(message, sender);
    }

    public static synchronized void broadcastDraftUpdate() {
        Message message = new Message(Message.Type.UPDATE_DRAFT, null, 
                null);
        broadcastMessage(message);
    }

    private static void broadcastToOthers(Message message, ClientHandler sender) {
        for (ClientHandler client : onlineUsers) {
            if (client != sender) {
                client.sendMessage(message);
            }
        }
    }

    private static void broadcastMessage(Message message) {
        for (ClientHandler client : onlineUsers) {
            client.sendMessage(message);
        }
    }


    

}