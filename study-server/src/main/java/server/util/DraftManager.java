package server.util;

import server.models.MainAction.Action;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;

public class DraftManager {
    private static final String DRAFT_FILE;
    private static boolean isBeingEdited = false;
    private static String currentEditor = null;
    
    // 全局操作历史
    private static List<Action> globalActions = new ArrayList<>();
    // 当前编辑用户的操作历史
    private static List<Action> currentEditActions = new ArrayList<>();
    // 当前编辑用户的撤销栈
    private static Stack<Action> undoStack = new Stack<>();
    // 当前编辑用户的重做栈
    private static Stack<Action> redoStack = new Stack<>();

    static {
        // 获取项目根目录
        String projectRoot = new File("").getAbsolutePath();
        // 设置草图文件路径
        DRAFT_FILE = Paths.get(projectRoot, "local_draft.ser").toString();
        
        // 确保文件存在并初始化
        try {
            File draftFile = new File(DRAFT_FILE);
            if (!draftFile.exists() || draftFile.length() == 0) {
                // 如果文件不存在或为空，创建文件并写入初始状态
                try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(draftFile))) {
                    DraftState initialState = new DraftState(new ArrayList<>());
                    oos.writeObject(initialState);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot initialize draft file", e);
        }
    }

    // 保存草图历史
    public static void saveDraftHistory() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(DRAFT_FILE))) {
            DraftState state = new DraftState(globalActions);
            oos.writeObject(state);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从文件加载历史记录
    // @SuppressWarnings("unchecked")
    public static void loadDraftHistory() {
        File file = new File(DRAFT_FILE);
        if (!file.exists() || file.length() == 0) {
            globalActions = new ArrayList<>();
            return;
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            DraftState state = (DraftState) ois.readObject();
            if (state != null) {
                globalActions = state.getGlobalActions();
            } else {
                globalActions = new ArrayList<>();
            }
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading draft history: " + e.getMessage());
            // 如果读取失败，初始化为空列表
            globalActions = new ArrayList<>();
            // 重新创建文件
            try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
                DraftState newState = new DraftState(globalActions);
                oos.writeObject(newState);
            } catch (IOException ex) {
                System.err.println("Error creating new draft file: " + ex.getMessage());
            }
        }
    }

    // 添加新的编辑动作
    public static synchronized void addEditAction(Action action, String username) {
        if (isBeingEdited && username.equals(currentEditor)) {
            currentEditActions.add(action);
            // 清空重做栈，因为新操作会使重做历史失效
            redoStack.clear();
        }
    }


    // 撤销操作
    public static synchronized Action undoAction(String username) {
        if (!isBeingEdited || !username.equals(currentEditor) || 
            currentEditActions.isEmpty()) {
            return null;
        }
        // 从当前操作列表中移除最后一个动作
        Action undoneAction = currentEditActions.remove(currentEditActions.size() - 1);
        // 将撤销的动作压入撤销栈
        undoStack.push(undoneAction);
        return undoneAction;
    }

    // 重做操作
    public static synchronized Action redoAction(String username) {
        if (!isBeingEdited || !username.equals(currentEditor) || 
            redoStack.isEmpty()) {
            return null;
        }
        // 从重做栈中弹出动作
        Action redoAction = redoStack.pop();
        // 将重做的动作添加回当前操作列表
        currentEditActions.add(redoAction);
        return redoAction;
    }

    // 提交编辑
    public static synchronized void commitEdit() {
        if (!isBeingEdited || currentEditor == null) {
            return;
        }
        // 更新全局历史
        globalActions.addAll(currentEditActions);
        // 保存到文件
        saveDraftHistory();
        // 清空所有临时状态
        currentEditActions.clear();
        undoStack.clear();
        redoStack.clear();
    }

    // 获取当前完整状态
    public static List<Action> getCurrentState() {
        List<Action> currentState = new ArrayList<>(globalActions);
        currentState.addAll(currentEditActions);
        return currentState;
    }

    // 清空所有历史记录
    public static synchronized void clear() {
        globalActions.clear();
        currentEditActions.clear();
        saveDraftHistory();
    }

    // Getter和Setter
    public static boolean isBeingEdited() {
        return isBeingEdited;
    }

    public static void setBeingEdited(boolean edited) {
        isBeingEdited = edited;
    }

    public static String getCurrentEditor() {
        return currentEditor;
    }

    public static void setCurrentEditor(String editor) {
        currentEditor = editor;
    }

    // 序列化状态的内部类
    private static class DraftState implements Serializable {
        private final List<Action> globalActions;

        public DraftState(List<Action> globalActions) {
            this.globalActions = globalActions;
        }

        public List<Action> getGlobalActions() {
            return globalActions;
        }
    }
}