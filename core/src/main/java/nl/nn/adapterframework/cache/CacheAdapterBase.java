/*
   Copyright 2013, 2016 Nationale-Nederlanden, 2020-2021 WeAreFrank!

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
package nl.nn.adapterframework.cache;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.Logger;
import org.springframework.context.ApplicationContext;

import lombok.Getter;
import lombok.Setter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.IConfigurationAware;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.util.LogUtil;
import nl.nn.adapterframework.util.TransformerPool;
import nl.nn.adapterframework.util.TransformerPool.OutputType;

/**
 * Baseclass for caching.
 * Provides key transformation functionality.
 *
 *
 * @author  Gerrit van Brakel
 * @since   4.11
 */
public abstract class CacheAdapterBase<V> implements ICache<String,V>, IConfigurationAware {
	protected Logger log = LogUtil.getLogger(this);
	private @Getter ClassLoader configurationClassLoader = Thread.currentThread().getContextClassLoader();
	private @Getter @Setter ApplicationContext applicationContext;

	private @Getter String name;

	private @Getter String keyXPath;
	private @Getter OutputType keyXPathOutputType=OutputType.TEXT;
	private @Getter String keyNamespaceDefs;
	private @Getter String keyStyleSheet;
	private @Getter String keyInputSessionKey;
	private @Getter boolean cacheEmptyKeys=false;

	private @Getter String valueXPath;
	private @Getter OutputType valueXPathOutputType=OutputType.XML;
	private @Getter String valueNamespaceDefs;
	private @Getter String valueStyleSheet;
	private @Getter String valueInputSessionKey;
	private @Getter boolean cacheEmptyValues=false;

	private TransformerPool keyTp=null;
	private TransformerPool valueTp=null;

	@Override
	public void configure(String ownerName) throws ConfigurationException {
		if (StringUtils.isEmpty(getName())) {
			setName(ownerName+"_cache");
		}
		if (StringUtils.isNotEmpty(getKeyXPath()) || StringUtils.isNotEmpty(getKeyStyleSheet())) {
			keyTp=TransformerPool.configureTransformer(this, getKeyNamespaceDefs(), getKeyXPath(), getKeyStyleSheet(), getKeyXPathOutputType(),false,null);
		}
		if (StringUtils.isNotEmpty(getValueXPath()) || StringUtils.isNotEmpty(getValueStyleSheet())) {
			valueTp=TransformerPool.configureTransformer(this, getValueNamespaceDefs(), getValueXPath(), getValueStyleSheet(), getValueXPathOutputType(),false,null);
		}
	}

	protected abstract V getElement(String key);
	protected abstract void putElement(String key, V value);
	protected abstract boolean removeElement(Object key);
	protected abstract V toValue(Message value);

	@Override
	public String transformKey(String input, PipeLineSession session) {
		if (StringUtils.isNotEmpty(getKeyInputSessionKey()) && session!=null) {
			input=(String)session.get(getKeyInputSessionKey());
		}
		if (keyTp!=null) {
			try {
				input=keyTp.transform(input, null);
			} catch (Exception e) {
				log.error(getLogPrefix()+"cannot determine cache key",e);
			}
		}
		if (StringUtils.isEmpty(input)) {
			log.debug("determined empty cache key");
			if (isCacheEmptyKeys()) {
				return "";
			}
			return null;
		}
		return input;
	}

	@Override
	public V transformValue(Message value, PipeLineSession session) {
		if (StringUtils.isNotEmpty(getValueInputSessionKey()) && session!=null) {
			value=Message.asMessage(session.get(getValueInputSessionKey()));
		}
		if (valueTp!=null) {
			try{
				value=new Message(valueTp.transform(value, null));
			} catch (Exception e) {
				log.error(getLogPrefix() + "transformValue() cannot transform cache value [" + value + "], will not cache", e);
				return null;
			}
		}
		if (value.isEmpty()) {
			log.debug("determined empty cache value");
			if (isCacheEmptyValues()) {
				return toValue(new Message(""));
			}
			return null;
		}
		return toValue(value);
	}

	@Override
	public V get(String key){
		return getElement(key);
	}
	@Override
	public void put(String key, V value) {
		putElement(key, value);
	}

	public boolean remove(String key) {
		return removeElement(key);
	}


	@IbisDoc({"name of the cache, will be lowercased", "<code>&lt;ownerName&gt;</code>_cache"})
	public void setName(String name) {
		if(StringUtils.isNotEmpty(name)) {
			this.name=name.toLowerCase();
		}
	}

	public String getLogPrefix() {
		return "cache ["+getName()+"] ";
	}

	@IbisDoc({"xpath expression to extract cache key from request message", ""})
	public void setKeyXPath(String keyXPath) {
		this.keyXPath = keyXPath;
	}

	@IbisDoc({"output type of xpath expression to extract cache key from request message", "text"})
	public void setKeyXPathOutputType(OutputType keyXPathOutputType) {
		this.keyXPathOutputType = keyXPathOutputType;
	}

	@IbisDoc({"namespace defintions for keyxpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setKeyNamespaceDefs(String keyNamespaceDefs) {
		this.keyNamespaceDefs = keyNamespaceDefs;
	}

	@IbisDoc({"stylesheet to extract cache key from request message. Use in combination with {@link #setCacheEmptyKeys(boolean) cacheEmptyKeys} to inhibit caching for certain groups of request messages", ""})
	public void setKeyStyleSheet(String keyStyleSheet) {
		this.keyStyleSheet = keyStyleSheet;
	}

	@IbisDoc({"session key to use as input for transformation of request message to key by keyxpath or keystylesheet", ""})
	public void setKeyInputSessionKey(String keyInputSessionKey) {
		this.keyInputSessionKey = keyInputSessionKey;
	}

	@IbisDoc({"controls whether empty keys are used for caching. when set true, cache entries with empty keys can exist.", "false"})
	public void setCacheEmptyKeys(boolean cacheEmptyKeys) {
		this.cacheEmptyKeys = cacheEmptyKeys;
	}

	@IbisDoc({"xpath expression to extract value to be cached key from response message. Use in combination with {@link #setCacheEmptyValues(boolean) cacheEmptyValues} to inhibit caching for certain groups of response messages", ""})
	public void setValueXPath(String valueXPath) {
		this.valueXPath = valueXPath;
	}
	public void setValueXPathOutputType(OutputType valueXPathOutputType) {
		this.valueXPathOutputType = valueXPathOutputType;
	}

	@IbisDoc({"namespace defintions for valuexpath. must be in the form of a comma or space separated list of <code>prefix=namespaceuri</code>-definitions", ""})
	public void setValueNamespaceDefs(String valueNamespaceDefs) {
		this.valueNamespaceDefs = valueNamespaceDefs;
	}

	@IbisDoc({"stylesheet to extract value to be cached from response message", ""})
	public void setValueStyleSheet(String valueStyleSheet) {
		this.valueStyleSheet = valueStyleSheet;
	}

	@IbisDoc({"session key to use as input for transformation of response message to cached value by valuexpath or valuestylesheet", ""})
	public void setValueInputSessionKey(String valueInputSessionKey) {
		this.valueInputSessionKey = valueInputSessionKey;
	}

	@IbisDoc({"controls whether empty values will be cached. when set true, empty cache entries can exist for any key.", "false"})
	public void setCacheEmptyValues(boolean cacheEmptyValues) {
		this.cacheEmptyValues = cacheEmptyValues;
	}

}
