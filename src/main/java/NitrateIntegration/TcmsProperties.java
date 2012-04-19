/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package NitrateIntegration;

import com.redhat.nitrate.Product;
import com.redhat.nitrate.TcmsConnection;
import com.redhat.nitrate.TestPlan;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    private Integer plan_id = null;
    private Integer product_id = null;
    private Integer product_v_id = null;
    private Integer category_id = null;
    private Integer priority_id = null;

    public TcmsProperties(String plan, String product, String product_v, String category, String priority) {
        this.plan = plan;
        this.product = product;
        this.product_v = product_v;
        this.category = category;
        this.priority = priority;
    }
    TcmsConnection connection;

    public void setConnection(TcmsConnection connection) {
        this.connection = connection;
    }

    public Integer planID() {
        if (plan_id == null) {
            TestPlan.get get = new TestPlan.get();
            get.id = Integer.parseInt(plan);
            try {
                Object o = connection.invoke(get);
                if (o instanceof XmlRpcStruct) {
                    TestPlan result = (TestPlan) TcmsConnection.rpcStructToFields((XmlRpcStruct) o, TestPlan.class);
                    plan_id = result.id;
                }
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return plan_id;
    }

    public Integer productID() {
        if (product_id == null) {
            Product.check_product get = new Product.check_product();
            get.name = product;
            try {
                Object o = connection.invoke(get);
                if (o instanceof XmlRpcStruct) {
                    Product result = (Product) TcmsConnection.rpcStructToFields((XmlRpcStruct) o, Product.class);
                    product_id = result.id;
                }
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return product_id;
    }

    public Integer product_vID() {
        if (product_v_id == null) {
            
        }
        return product_v_id;
    }

    public Integer categoryID() {
        if (category_id == null) {
               TestPlan.get get = new TestPlan.get();
            get.id = Integer.parseInt(plan);
            try {
                Object o = connection.invoke(get);
                if (o instanceof XmlRpcStruct) {
                    TestPlan plan = (TestPlan) TcmsConnection.rpcStructToFields((XmlRpcStruct) o, TestPlan.class);
                    plan_id = plan.id;
                }
            } catch (XmlRpcFault ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            } catch (InstantiationException ex) {
                Logger.getLogger(TcmsProperties.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        return category_id;
    }

    public Integer priorityID() {
        if (priority_id == null) {
        }
        return priority_id;
    }
}
