package org.jbpm.prediction.randomforest;

import java.util.Objects;

public class Approval {

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Approval approval = (Approval) o;
        return level == approval.level &&
                Objects.equals(user, approval.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, level);
    }

    final private String user;

    public String getUser() {
        return user;
    }

    public int getLevel() {
        return level;
    }

    final private int level;

    private Approval(String user, int level) {
        this.user = user;
        this.level = level;
    }

    public static Approval create(String user, int level) {
        return new Approval(user, level);
    }
}
