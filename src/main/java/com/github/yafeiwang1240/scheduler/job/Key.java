package com.github.yafeiwang1240.scheduler.job;

import java.io.Serializable;
import java.util.UUID;

public class Key<T> implements Serializable, Comparable<Key<T>>  {

    public static final String DEFAULT_GROUP = "DEFAULT_GROUP";

    private final String name;
    private final String group;

    public Key(String name, String group) {
        if(name == null)
            throw new IllegalArgumentException("Name cannot be null.");
        this.name = name;
        if(group != null)
            this.group = group;
        else
            this.group = DEFAULT_GROUP;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((group == null) ? 0 : group.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        @SuppressWarnings("unchecked")
        Key<T> other = (Key<T>) obj;
        if (group == null) {
            if (other.group != null)
                return false;
        } else if (!group.equals(other.group))
            return false;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    public int compareTo(Key<T> o) {

        if(group.equals(DEFAULT_GROUP) && !o.group.equals(DEFAULT_GROUP))
            return -1;
        if(!group.equals(DEFAULT_GROUP) && o.group.equals(DEFAULT_GROUP))
            return 1;

        int r = group.compareTo(o.getGroup());
        if(r != 0)
            return r;

        return name.compareTo(o.getName());
    }

    public String getFullName() {
        return group + "." + name;
    }

    public static String createUniqueName(String group) {
        if(group == null)
            group = DEFAULT_GROUP;

        String n1 = UUID.randomUUID().toString();
        String n2 = UUID.nameUUIDFromBytes(group.getBytes()).toString();

        return String.format("%s-%s", n2.substring(24), n1);
    }
}
