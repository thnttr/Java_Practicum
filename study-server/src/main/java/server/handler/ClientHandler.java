package server.handler;

import server.ServerMain;
import server.models.MainAction.Action;
import server.models.MainAction;
import server.models.Message;
import server.models.UserSession;
import server.util.DraftManager;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private UserSession userSession;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            input = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            closeConnection();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Message message = (Message) input.readObject();
                handleMessage(message);
            }
        } catch (IOException | ClassNotFoundException e) {
            ServerMain.handleClientExit(this);
            closeConnection();
        }
    }

    private void handleMessage(Message message) {
        switch (message.getCommand()) {
            case REGISTER:
                userSession = (UserSession) message.getData();
                ServerMain.handleClientRegistration(this, userSession);
                break;
            case INITIAL_STATE:
                initialState();
            case EDIT_REQUEST:
                handleEditRequest();
                break;
            case EDIT_ACTION:
                handleEditAction((Action) message.getData());
                break;
            case EDIT_COMPLETE:
                handleEditComplete();
                break;
            case EXIT:
                ServerMain.handleClientExit(this);
                closeConnection();
                break;
            default: 
                break;
        }
    }

    public void initialState() {
        Message message = new Message(Message.Type.INITIAL_STATE, DraftManager.getCurrentState(),null);
        sendMessage(message);
    }

    private void handleEditRequest() {
        boolean granted = ServerMain.requestEditPermission(this);
        Message response = new Message(
            granted ? Message.Type.EDIT_GRANTED : Message.Type.EDIT_REJECTED,
            null,
            granted ? userSession.getUsername() : DraftManager.getCurrentEditor()
        );
        sendMessage(response);
    }

    private void handleEditAction(Action action) {
        if (action instanceof MainAction.UndoAction){
            handleUndo();
            return;
        }else if (action instanceof MainAction.RedoAction){
            handleRedo();
            return;
        }else {
            DraftManager.addEditAction(action, userSession.getUsername());
        }
        ServerMain.broadcastEditAction(action, this);
    }

    private void handleEditComplete() {
        ServerMain.handleEditComplete();
    }

    private void handleUndo() {
        Action undoneAction = DraftManager.undoAction(userSession.getUsername());
        if (undoneAction != null) {
            ServerMain.broadcastEditAction(new MainAction.UndoAction(), this);
        }
    }

    private void handleRedo() {
        Action redoAction = DraftManager.redoAction(userSession.getUsername());
        if (redoAction != null) {
            ServerMain.broadcastEditAction(new MainAction.RedoAction(), this);
        }
    }

    public void sendMessage(Message message) {
        try {
            output.writeObject(message);
            output.flush();
        } catch (IOException e) {
            closeConnection();
        }
    }

    public Socket getSocket() {
        return socket;
    }

    public UserSession getUserSession() {
        return userSession;
    }

    public void closeConnection() {
        try {
            if (input != null) input.close();
            if (output != null) output.close();
            if (socket != null) socket.close();
        } catch (IOException e) {
        }
    }
}