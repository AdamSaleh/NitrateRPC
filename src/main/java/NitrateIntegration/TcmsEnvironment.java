/*
 * Copyright (C) 2012 Red Hat, Inc.     
 * 
 * This copyrighted material is made available to anyone wishing to use, 
 * modify, copy, or redistribute it subject to the terms and conditions of the 
 * GNU General Public License v.2.
 * 
 * Authors: Adam Saleh (asaleh at redhat dot com)
 *          Jan Rusnacko (jrusnack at redhat dot com)
 */

package NitrateIntegration;

import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.TcmsException;
import com.redhat.nitrate.command.Env;
import com.redhat.nitrate.command.Env.Value;
import java.util.Hashtable;
import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcStruct;

/**
 *
 * @author asaleh
 */
public class TcmsEnvironment {

    public final String env;
    private Integer envId;
    private Env.Group env_obj;
    private Hashtable<String, Env.Property> properties;
    private Hashtable<Integer, Env.Value> values_by_id;
    private Hashtable<String, Hashtable<String, Env.Value>> values;

    public TcmsEnvironment(String env) {
        properties = new Hashtable<String, Env.Property>();
        values = new Hashtable<String, Hashtable<String, Value>>();
        values_by_id = new Hashtable<Integer, Env.Value>();
        this.env = env;
    }

    public String getEnv() {
        return env;
    }

    public Integer getEnvId() {
        return envId;
    }
    TcmsConnection connection;

    public void setConnection(TcmsConnection connection) {
        this.connection = connection;
    }

    /**
     * Gets EnvID of
     * <code>env</code> from tcms server. Call this if you need to check whether
     * <code>env</code> is present on server - don`t do
     * <code>reload</code>.
     *
     * @throws TcmsException
     */
    public void reloadEnvId() throws TcmsException {
        if (!isEmpty()) {

            envId = null;
            Env.filter_groups get = new Env.filter_groups();
            get.name = env;
            Object o = connection.invoke(get);
            if (o instanceof XmlRpcArray) {
                if (((XmlRpcArray) o).size() == 0) {
                    envId = null;
                    return;
                }
                o = ((XmlRpcArray) o).get(0);
            }
            if (o instanceof XmlRpcStruct) {
                env_obj = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, Env.Group.class);
                envId = env_obj.id;
            }
        }
    }

    /**
     * Gets all availables properties of
     * <code>env</code> from server.
     *
     * @throws TcmsException
     */
    public void fetchAvailableProperties() throws TcmsException {
        if (!isEmpty()) {

            Env.get_properties get = new Env.get_properties();
            get.env_group_id = env_obj.id;
            Object o = connection.invoke(get);
            if (o instanceof XmlRpcArray) {
                XmlRpcArray rpcArray = (XmlRpcArray) o;
                for (Object pr : rpcArray) {
                    if (pr instanceof XmlRpcStruct) {
                        XmlRpcStruct pr_str = (XmlRpcStruct) pr;
                        Env.Property pr_obj = TcmsConnection.rpcStructToFields(pr_str, Env.Property.class);
                        properties.put(pr_obj.name, pr_obj);
                    }
                }
            }

        }
    }

    /**
     * Gets all values of all properties of
     * <code>env</code> from server. Might be inefficient - consider using
     * <code>reloadProperty</code> instead.
     *
     * @throws TcmsException
     */
    public void reloadAllProperties() throws TcmsException {
        for (Env.Property property : properties.values()) {
            Env.get_values get = new Env.get_values();
            get.env_property_id = property.id;
            Object o = connection.invoke(get);
            Hashtable<String, Env.Value> value_set = new Hashtable<String, Env.Value>();
            if (o instanceof XmlRpcArray) {
                XmlRpcArray rpcArray = (XmlRpcArray) o;
                for (Object vl : rpcArray) {
                    if (vl instanceof XmlRpcStruct) {
                        XmlRpcStruct vl_str = (XmlRpcStruct) vl;
                        Env.Value vl_obj = TcmsConnection.rpcStructToFields(vl_str, Env.Value.class);
                        value_set.put(vl_obj.value, vl_obj);
                        values_by_id.put(vl_obj.id, vl_obj);
                    }
                }
            }
            values.put(property.name, value_set);
        }
    }

    /**
     * Gets all values available for property specified.
     *
     * @param property
     * @throws TcmsException
     */
    public void reloadProperty(String property) throws TcmsException {
        if (!properties.containsKey(property)) {
            throw new IllegalArgumentException("No such property");
        }

        Env.get_values get = new Env.get_values();
        get.env_property_id = properties.get(property).id;
        Object o = connection.invoke(get);
        Hashtable<String, Env.Value> value_set = new Hashtable<String, Env.Value>();
        if (o instanceof XmlRpcArray) {
            XmlRpcArray rpcArray = (XmlRpcArray) o;
            for (Object vl : rpcArray) {
                if (vl instanceof XmlRpcStruct) {
                    XmlRpcStruct vl_str = (XmlRpcStruct) vl;
                    Env.Value vl_obj = TcmsConnection.rpcStructToFields(vl_str, Env.Value.class);
                    value_set.put(vl_obj.value, vl_obj);
                    values_by_id.put(vl_obj.id, vl_obj);
                }
            }
        }
        values.put(properties.get(property).name, value_set);
    }

    public Hashtable<String, Hashtable<String, Value>> getValues() {
        return values;
    }

    public Hashtable<String, Env.Property> getProperties() {
        return properties;
    }

    public Env.Value getValueById(Integer i) {
        return values_by_id.get(i);
    }

    public boolean containsProperty(String property) {
        if (properties != null) {
            return properties.containsKey(property);
        }
        return false;
    }

    public boolean containsValue(String property, String value) {
        if (containsProperty(property) == false) {
            return false;
        }
        return values.get(property).containsKey(value);
    }

    /*
     * Returns true if environment is not initialized with any value
     */
    public boolean isEmpty() {
        if (env != null) {
            return env.isEmpty();
        }
        return true;
    }
}
