package org.jbpm.prediction.randomforest;

import java.util.Objects;

public class Approval {




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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Approval approval = (Approval) o;

        if (level != approval.level) return false;
        return user.equals(approval.user);
    }

    @Override
    public int hashCode() {
        int result = user.hashCode();
        result = 31 * result + level;
        return result;
    }
}
