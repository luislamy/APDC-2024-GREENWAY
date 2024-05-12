package pt.unl.fct.di.apdc.projeto.resources;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.codec.digest.DigestUtils;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.StringValue;
import com.google.cloud.datastore.Transaction;

import pt.unl.fct.di.apdc.projeto.util.ServerConstants;

public class AdminUserInitializer extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    /** Logger Object */
	private static final Logger LOG = Logger.getLogger(LoginResource.class.getName());

	/** Class that stores the server constants to use in operations */
	public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

	/** The data store to store users in */
	private static final Datastore datastore = serverConstants.getDatastore();

    @Override
    public void init() throws ServletException {
        Key rootKey = serverConstants.getUserKey("root");
        Transaction txn = datastore.newTransaction();
        try {
            Entity root = txn.get(rootKey);
            if ( root == null ) {
                root = Entity.newBuilder(rootKey)
                        .set("username", "root")
						.set("password", DigestUtils.sha3_512Hex("password"))
						.set("email", "root")
						.set("name", "root")
						.set("phone", "root")
						.set("profile", ServerConstants.PRIVATE)
						.set("work", "root")
						.set("workplace", "root")
						.set("address", "root")
						.set("postalcode", "root")
						.set("fiscal", "root")
						.set("role", ServerConstants.SU)
						.set("state", ServerConstants.ACTIVE)
						.set("userCreationTime", Timestamp.now())
						.set("photo", StringValue.newBuilder("").setExcludeFromIndexes(true).build())
						.build();
                txn.put(root);
                txn.commit();
                LOG.fine("Root register: root user was registered in the database.");
            }
        } catch ( Exception e ) {
			txn.rollback();
			LOG.severe("Root register: " + e.getMessage());
		} finally {
			if (txn.isActive()) {
				txn.rollback();
                LOG.severe("Root register: Internal server error.");
			}
		}
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("text/plain");
        resp.getWriter().println("Root user registration complete.");
    }
}