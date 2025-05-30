# Java_Practicum

## 客户端
```
study-client/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── client/   
│   │   │        ├── controller/
│   │   │        │   ├── MainController.java             #  主界面控制器，界面与事件关联    
│   │   │        │   └── ...    
│   │   │        ├── util/    
│   │   │        │   ├── FileLoader.java                 # 本地文件更新、远端文件发送模块    
│   │   │        │   └── NetworkClient.java              # 网络通信模块    
│   │   │        ├── view/    
│   │   │        │   └── main.fxml                       # JavaFX界面文件    
│   │   │        └── App.java                            # JavaFX应用入口                 
│   │   └── resources/
│   │       ├── client/view/
│   │       │   └── main.css                             # 界面样式表
│   │       └── config.properties                        # 客户端配置(可选)
│   └── test/
│       └── java/                                        # 测试代码
├── target/                                              # 存放.clsss
├── local_draft.xml/json/csv                             # 客户端保存的草图
└── pom.xml
```
## 服务器
```
study-server/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── server/
│   │   │        ├── handler/
│   │   │        │   ├── ClientHandler.java              # 客户端连接处理器    
│   │   │        │   └── ActionController.java           # 客户端行为控制    
│   │   │        ├── models/
│   │   │        │   ├── Message.java                    
│   │   │        │   └── UserSession.java                # 用户信息    
│   │   │        ├── util/
│   │   │        │   ├── UserManger.java                 # 管理用户信息，以及用户文件的读写
│   │   │        │   └── DraftManager.java               # 管理本地草图，以及本地草图文件的读写    
│   │   │        └── ServerMain.java                     # 服务端启动类
│   │   └── resources/
│   │       └── server-config.properties                 # 服务端配置（可选）
│   └── test/
│       └── java/                                        # 测试代码
├── target/                                              # 存放.clsss
├── local_draft.xml/json/csv                             # 服务器端保存的草图
├── db/                                                  # 用户信息
└── pom.xml
```
