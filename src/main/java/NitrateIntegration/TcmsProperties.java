/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import redstone.xmlrpc.XmlRpcArray;
import redstone.xmlrpc.XmlRpcFault;
import redstone.xmlrpc.XmlRpcStruct;

/**
 *
 * @author asaleh
 */
public class TcmsProperties {

    public final String plan;
    public final String product;
    public final String product_v;
    public final String category;
    public final String priority;
    public final String manager;
    private Integer plan_id = null;
    private Integer product_id = null;
    private Integer product_v_id = null;
    private Integer category_id = null;
    private Integer priority_id = null;
    private Integer manager_id = null;

    public TcmsProperties(String plan, String product, String product_v, String category, String priority, String manager) {
        this.plan = plan;
        this.product = product;
        this.product_v = product_v;
        this.category = category;
        this.priority = priority;
        this.manager = manager;
    }
    TcmsConnection connection;

    public void setConnection(TcmsConnection connection) {
        this.connection = connection;
    }

    public void reload() {
        try {
            reloadPlanId();
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            reloadProductId();
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            reloadProduct_vId();
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            reloadPriorityId();
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            reloadCategoryId();
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
        try {
            reloadManagerId();
        } catch (XmlRpcFault ex) {
            Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public Integer getPlanID() {
        return plan_id;
    }

    public void reloadPlanId() throws XmlRpcFault {
        plan_id = null;
        TestPlan.get get = new TestPlan.get();
        try {
            get.id = Integer.parseInt(plan);

        } catch (Exception e) {//plan is not numeric
            return;
        }
        Object o = connection.invoke(get);

        if (o instanceof XmlRpcStruct) {
            TestPlan result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, TestPlan.class);
            plan_id = result.plan_id;
        }

    }

    public Integer getProductID() {
        return product_id;
    }

    public void reloadProductId() throws XmlRpcFault {
        product_id = null;
        Product.check_product get = new Product.check_product();
        get.name = product;
        Object o = connection.invoke(get);
        if (o instanceof XmlRpcStruct) {
            Product result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, Product.class);
            product_id = result.id;
        }

    }

    public void reloadProduct_vId() throws XmlRpcFault {
        product_v_id = null;
        Product.get_versions get = new Product.get_versions();
        get.id_str = product;
        Object a = connection.invoke(get);
        if (a instanceof XmlRpcArray) {
            XmlRpcArray array = (XmlRpcArray) a;
            for (Object o : array) {
                if (o instanceof XmlRpcStruct) {
                    Product.Version result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, Product.Version.class);
                    if (result.value.contentEquals(product_v)) {
                        product_v_id = result.id;
                        return;
                    }
                }
            }
        }

    }

    public Integer getProduct_vID() {
        return product_v_id;
    }

    public void reloadCategoryId() throws XmlRpcFault {
        category_id = null;
        Product.check_category get = new Product.check_category();
        get.name = category;
        get.product = product_id;
        Object o = connection.invoke(get);
        if (o instanceof XmlRpcStruct) {
            Product.Category result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, Product.Category.class);
            category_id = result.id;
        }


    }

    public Integer getCategoryID() {

        return category_id;
    }

    public void reloadPriorityId() throws XmlRpcFault {
        priority_id = null;
        TestCase.check_priority get = new TestCase.check_priority();
        get.value = priority;
        Object o = connection.invoke(get);
        if (o instanceof XmlRpcStruct) {
            TestCase.Priority result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, TestCase.Priority.class);
            priority_id = result.id;
        }

    }

    public Integer getPriorityID() {

        return priority_id;
    }

    public void reloadManagerId() throws XmlRpcFault {
        manager_id = null;
        User.filter get = new User.filter();
        get.username__startswith = manager;
        Object o = connection.invoke(get);
        if (o instanceof XmlRpcArray) {
            if (((XmlRpcArray) o).size() == 0) {
                manager_id = null;
                return;
            }
            o = ((XmlRpcArray) o).get(0);
        }
        if (o instanceof XmlRpcStruct) {
            User result = TcmsConnection.rpcStructToFields((XmlRpcStruct) o, User.class);
            manager_id = result.id;
        }

    }

    public Integer getManagerId() {
        return manager_id;
    }

    public String getCategory() {
        return category;
    }

    public String getManager() {
        return manager;
    }

    public String getPlan() {
        return plan;
    }

    public String getPriority() {
        return priority;
    }

    public String getProduct() {
        return product;
    }

    public String getProduct_v() {
        return product_v;
    }
}
