/*
   Copyright 2019-2021 WeAreFrank!

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
package nl.nn.adapterframework.ldap;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.naming.Context;
import javax.naming.NamingException;

import nl.nn.adapterframework.cache.ICache;
import nl.nn.adapterframework.cache.ICacheEnabled;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeRunResult;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.XmlBuilder;

/**
 * Pipe that returns the memberships of a userDN.
 * The input is a fullDn, of a user or a group.
 * <br/>
 * Sample result:<br/><code><pre>
 *	&lt;ldap&gt;
 *	 &lt;entry name="CN=xxyyzz,OU=Users,DC=domain,DC=ext"&gt;
 *	   &lt;attributes&gt;
 *	    &lt;attribute&gt;
 *	    &lt;attribute name="memberOf" value="Extern"/&gt;
 *	    &lt;attribute name="departmentCode" value="358000"/&gt;
 *	    &lt;attribute name="organizationalHierarchy"&gt;
 *	        &lt;item value="ou=zzyyxx"/&gt;
 *	        &lt;item value="ou=OPS&amp;IT,ou=Group,ou=domain,o=ext"/&gt;
 *	    &lt;/attribute>
 *	    &lt;attribute name="givenName" value="Gerrit"/>
 *	   &lt;/attributes&gt;
 *	  &lt;/entry&gt;
 *   &lt;entry&gt; .... &lt;/entry&gt;
 *   .....
 *	&lt;/ldap&gt;
 * </pre></code> <br/>
 *
 * @author Gerrit van Brakel
 */
public class LdapFindGroupMembershipsPipe extends LdapQueryPipeBase implements ICacheEnabled<String,Set<String>> {

	private boolean recursiveSearch = true;

	private LdapClient ldapClient;
	private ICache<String, Set<String>> cache;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		Map<String,Object> options=new HashMap<String,Object>();
		options.put("java.naming.provider.url",retrieveUrl(getHost(), getPort(), getBaseDN(), isUseSsl()));
		options.put(Context.SECURITY_AUTHENTICATION, "simple");
		options.put(Context.SECURITY_PRINCIPAL, cf.getUsername());
		options.put(Context.SECURITY_CREDENTIALS, cf.getPassword());
		ldapClient= new LdapClient(options);
		ldapClient.setCache(cache);
		ldapClient.configure();
	}

	@Override
	public void start() throws PipeStartException {
		super.start();
		try {
			ldapClient.open();
		} catch (SenderException e) {
			throw new PipeStartException(e);
		}
	}

	@Override
	public void stop() {
		try {
			ldapClient.close();
		} catch (SenderException e) {
			log.warn("cannot close ldapClient",e);
		} finally {
			super.stop();
		}
	}


	@Override
	public PipeRunResult doPipeWithException(Message message, PipeLineSession session) throws PipeRunException {
		if (message==null) {
			throw new PipeRunException(this, "input is null");
		}

		String searchedDN;
		try {
			searchedDN = message.asString();
		} catch (IOException e) {
			throw new PipeRunException(this, "Failure converting input to string", e);
		}

		Set<String> memberships;
		try {
			if (isRecursiveSearch()) {
				memberships= ldapClient.searchRecursivelyViaAttributes(searchedDN, getBaseDN(), "memberOf");
			} else {
				memberships= ldapClient.searchObjectForMultiValuedAttribute(searchedDN, getBaseDN(), "memberOf");
			}
			XmlBuilder result = new XmlBuilder("ldap");
			result.addSubElement("entryName", searchedDN);
			XmlBuilder attributes = new XmlBuilder("attributes");
			result.addSubElement(attributes);
			for (String membership:memberships) {
				XmlBuilder attribute = new XmlBuilder("attribute");
				attribute.addAttribute("attrID", "memberOf");
				attribute.setValue(membership,true);
				attributes.addSubElement(attribute);
			}
			return new PipeRunResult(getSuccessForward(), result.toXML());
		} catch (NamingException e) {
			throw new PipeRunException(this, "exception on ldap lookup", e);
		}
	}


	@Override
	public void setCache(ICache<String, Set<String>> cache) {
		this.cache=cache;
	}
	@Override
	public ICache<String, Set<String>> getCache() {
		return cache;
	}

	@IbisDoc({"when <code>true</code>, the memberOf attribute is also searched in all the found members", "true"})
	public void setRecursiveSearch(boolean b) {
		recursiveSearch = b;
	}
	public boolean isRecursiveSearch() {
		return recursiveSearch;
	}

}
