package org.gluu.oxtrust.api.server.api.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.gluu.oxtrust.api.server.util.Constants;
import org.gluu.oxtrust.ldap.service.ConfigurationService;
import org.gluu.oxtrust.ldap.service.EncryptionService;
import org.gluu.oxtrust.model.GluuConfiguration;
import org.gluu.oxtrust.service.filter.ProtectedApi;
import org.gluu.oxtrust.api.server.util.ApiConstants;
import org.slf4j.Logger;
import org.gluu.model.SmtpConfiguration;
import org.gluu.service.MailService;

import com.google.common.base.Preconditions;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path(ApiConstants.BASE_API_URL + ApiConstants.CONFIGURATION + ApiConstants.SMTP)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = ApiConstants.BASE_API_URL + ApiConstants.CONFIGURATION
		+ ApiConstants.SMTP, description = "Smtp server configuration web service")
@ApplicationScoped
public class SmtpConfigurationWebResource extends BaseWebResource {
	@Inject
	private Logger logger;

	@Inject
	private ConfigurationService configurationService;

	@Inject
	private MailService mailService;

	@Inject
	private EncryptionService encryptionService;

	@GET
	@ApiOperation(value = "Get smtp configuration")
	@ApiResponses(value = {
			@ApiResponse(code = 200, response = SmtpConfiguration.class, message = Constants.RESULT_SUCCESS),
			@ApiResponse(code = 500, message = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response getSmtpServerConfiguration() {
		try {
			SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
			if (smtpConfiguration != null) {
				return Response.ok(smtpConfiguration).build();
			} else {
				return Response.ok(Response.Status.NOT_FOUND).build();
			}
		} catch (Exception e) {
			log(logger, e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@PUT
	@ApiOperation(value = "Update smtp configuration")
	@ApiResponses(value = {
			@ApiResponse(code = 200, response = SmtpConfiguration.class, message = Constants.RESULT_SUCCESS),
			@ApiResponse(code = 404, message = "Not found"), @ApiResponse(code = 500, message = "Server error") })
	@ProtectedApi(scopes = { WRITE_ACCESS })
	public Response updateSmtpConfiguration(SmtpConfiguration smtpConfiguration) {
		try {
			Preconditions.checkNotNull(smtpConfiguration, "Attempt to update null smtpConfiguration");
			configurationService.encryptedSmtpPassword(smtpConfiguration);
			GluuConfiguration configurationUpdate = configurationService.getConfiguration();
			configurationUpdate.setSmtpConfiguration(smtpConfiguration);
			configurationService.updateConfiguration(configurationUpdate);
			return Response.ok(configurationService.getConfiguration().getSmtpConfiguration()).build();
		} catch (Exception e) {
			log(logger, e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Path(ApiConstants.TEST)
	@ApiOperation(value = "Test smtp configuration")
	@ApiResponses(value = {
			@ApiResponse(code = 200, response = SmtpConfiguration.class, message = Constants.RESULT_SUCCESS),
			@ApiResponse(code = 500, message = "Server error") })
	@ProtectedApi(scopes = { READ_ACCESS })
	public Response testSmtpConfiguration() {
		try {
			SmtpConfiguration smtpConfiguration = configurationService.getConfiguration().getSmtpConfiguration();
			String password = encryptionService.decrypt(smtpConfiguration.getPassword());
			smtpConfiguration.setPasswordDecrypted(password);
			boolean result = mailService.sendMail(smtpConfiguration, smtpConfiguration.getFromEmailAddress(),
					smtpConfiguration.getFromName(), smtpConfiguration.getFromEmailAddress(), null,
					"SMTP Configuration verification", "Mail to test smtp configuration",
					"Mail to test smtp configuration");
			return Response.ok(result ? true : false).build();
		} catch (Exception e) {
			log(logger, e);
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
		}
	}

}
