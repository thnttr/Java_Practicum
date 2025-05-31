// study-server/src/main/java/server/models/UserSession.java
package server.models;

import java.io.Serializable;

public class UserSession implements Serializable {
    private String username;
    private String studentId;
    private String ipAddress;

    public UserSession(String username, String studentId, String ipAddress) {
        this.username = username;
        this.studentId = studentId;
        this.ipAddress = ipAddress;
    }

    public String getUsername() {
        return username;
    }

    public String getStudentId() {
        return studentId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserSession that = (UserSession) o;
        return studentId.equals(that.studentId);
    }

    @Override
    public int hashCode() {
        return studentId.hashCode();
    }
}