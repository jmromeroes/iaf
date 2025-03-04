/*
Copyright 2016-2022 WeAreFrank!

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package nl.nn.adapterframework.webcontrol.api;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;

import nl.nn.adapterframework.management.bus.BusAction;
import nl.nn.adapterframework.management.bus.BusTopic;
import nl.nn.adapterframework.management.bus.RequestMessageBuilder;
import nl.nn.adapterframework.util.FileUtils;
import nl.nn.adapterframework.webcontrol.FileViewerServlet;

/**
 * Shows directory of logfiles
 * 
 * @since	7.0-B1
 * @author	Niels Meijer
 */

@Path("/")
public class ShowLogging extends FrankApiBase {

	@GET
	@RolesAllowed({"IbisObserver", "IbisDataAdmin", "IbisAdmin", "IbisTester"})
	@Path("/logging")
	@Relation("logging")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLogDirectory(@QueryParam("directory") String directory, @QueryParam("sizeFormat") String sizeFormat, @QueryParam("wildcard") String wildcard) {
		if(StringUtils.isNotEmpty(directory) && !FileUtils.readAllowed(FileViewerServlet.permissionRules, getServletRequest(), directory)) {
			throw new ApiException("Access to path (" + directory + ") not allowed!");
		}

		RequestMessageBuilder builder = RequestMessageBuilder.create(this, BusTopic.LOGGING, BusAction.GET);
		builder.addHeader("directory", directory);
		builder.addHeader("sizeFormat", sizeFormat);
		builder.addHeader("wildcard", wildcard);
		return callSyncGateway(builder);
	}
}
