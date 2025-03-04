/*
   Copyright 2018, 2020 Nationale-Nederlanden, 2021-2022 WeAreFrank!

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
package nl.nn.ibistesttool;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import nextapp.echo2.app.ApplicationInstance;
import nextapp.echo2.webcontainer.WebContainerServlet;
import nl.nn.adapterframework.lifecycle.DynamicRegistration;
import nl.nn.adapterframework.lifecycle.IbisInitializer;
import nl.nn.adapterframework.lifecycle.ServletManager;
import nl.nn.testtool.echo2.Echo2Application;

/**
 * @author Jaco de Groot
 */
@IbisInitializer
public class Servlet extends WebContainerServlet implements DynamicRegistration.Servlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see nl.nn.testtool.echo2.Echo2Application#initBean()
	 */
	@Override
	public ApplicationInstance newApplicationInstance() {
		WebApplicationContext webApplicationContext = WebApplicationContextUtils.getWebApplicationContext(getServletContext());
		return webApplicationContext.getBean("echo2Application", Echo2Application.class);
	}

	@Override
	public String getUrlMapping() {
		return "/iaf/testtool";
	}

	@Override
	public String getName() {
		return "TestTool";
	}

	@Override
	public String[] getAccessGrantingRoles() {
		return ALL_IBIS_USER_ROLES;
	}

	@Autowired
	public void setServletManager(ServletManager servletManager) {
		servletManager.register(this);
	}
}
