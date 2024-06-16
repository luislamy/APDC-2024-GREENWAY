package pt.unl.fct.di.apdc.projeto.resources;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.Transaction;
import com.google.gson.Gson;
import pt.unl.fct.di.apdc.projeto.util.ApplicationData;
import pt.unl.fct.di.apdc.projeto.util.AuthToken;
import pt.unl.fct.di.apdc.projeto.util.ServerConstants;
import pt.unl.fct.di.apdc.projeto.util.Validations;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import java.util.UUID;
import java.util.logging.Logger;

@Path("/applications")
public class ApplicationResource {

    /**
     * Logger Object
     */
    private static Logger LOG = Logger.getLogger(ApplicationResource.class.getName());

    /**
     * Class that stores the server constants to use in operations
     */
    public static final ServerConstants serverConstants = ServerConstants.getServerConstants();

    /**
     * The data store to store users in
     */
    private static final Datastore datastore = serverConstants.getDatastore();

    /**
     * The converter to JSON
     */
    private final Gson g = new Gson();

    public ApplicationResource() {
    }

    @POST
    @Path("/submit")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submitApplication(@HeaderParam("authToken") String jsonToken, ApplicationData data) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        LOG.fine("Submit application: Attempt to submit application by: " + authToken.username + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            var validation = Validations.checkApplicationsValidations(Validations.SUBMIT_APPLICATION, user, token, authToken, data);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(authToken.username, authToken.tokenID);
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                return validation;
            } else {
                String applicationID;
                do {
                    applicationID = UUID.randomUUID().toString();
                } while (serverConstants.getApplication(txn, applicationID) != null);
                Entity application = Entity.newBuilder(serverConstants.getApplicationKey(applicationID))
                        .set("applicationID", applicationID)
                        .set("companyName", data.companyName)
                        .set("slogan", data.slogan)
                        .set("description", data.description)
                        .set("submitterName", authToken.username)
                        .set("city", data.city)
                        .set("country", data.country)
                        .set("address", data.address)
                        .set("contact", data.contact)
                        .set("isAccepted", false)
                        .set("creationDate", Timestamp.now())
                        .build();
                txn.add(application);
                txn.commit();
                LOG.fine("Submit application: " + data.companyName + "'s submission was registered in the database.");
                return Response.ok().build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Submit application: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Submit application: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

    @POST
    @Path("/process")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response processApplication(@HeaderParam("authToken") String
                                               jsonToken, @PathParam("applicationID") String applicationID, boolean isAccepted) {
        AuthToken authToken = g.fromJson(jsonToken, AuthToken.class);
        String accept = isAccepted ? "accept" : "decline";
        LOG.fine("Process application: Attempt to " + accept + " application by: " + authToken.username + ".");
        Transaction txn = datastore.newTransaction();
        try {
            Entity user = serverConstants.getUser(txn, authToken.username);
            Entity token = serverConstants.getToken(txn, authToken.username, authToken.tokenID);
            Entity application = serverConstants.getApplication(txn, applicationID);
            Entity submitter = application == null ? null : serverConstants.getUser(txn, application.getString("submitterName"));
            var validation = Validations.checkApplicationsValidations(Validations.PROCESS_APPLICATION, user, submitter, token,
                    authToken, isAccepted);
            if (validation.getStatus() == Status.UNAUTHORIZED.getStatusCode()) {
                serverConstants.removeToken(authToken.username, authToken.tokenID);
                return validation;
            } else if (validation.getStatus() != Status.OK.getStatusCode()) {
                return validation;
            } else {

                submitter = isAccepted ?
                        Entity.newBuilder(submitter)
                                .set("role", ServerConstants.EP)
                                .build()
                        : submitter;
                application = Entity.newBuilder(serverConstants.getApplicationKey(applicationID))
                        .set("applicationID", application.getString("applicationID"))
                        .set("companyName", application.getString("companyName"))
                        .set("slogan", application.getString("slogan"))
                        .set("description", application.getString("description"))
                        .set("submitterName", application.getString("submitterName"))
                        .set("city", application.getString("city"))
                        .set("country", application.getString("country"))
                        .set("address", application.getString("address"))
                        .set("contact", application.getString("contact"))
                        .set("isAccepted", isAccepted)
                        .set("creationDate", application.getTimestamp("creationDate"))
                        .build();
                txn.update(application, submitter);
                txn.commit();
                LOG.fine("Process application: " + authToken.username + "'s submission was processed.");
                return Response.ok().build();
            }
        } catch (Exception e) {
            txn.rollback();
            LOG.severe("Process application: " + e.getMessage());
            return Response.status(Status.INTERNAL_SERVER_ERROR).build();
        } finally {
            if (txn.isActive()) {
                txn.rollback();
                LOG.severe("Process application: Internal server error.");
                return Response.status(Status.INTERNAL_SERVER_ERROR).build();
            }
        }
    }

}
