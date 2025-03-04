/*
Copyright 2020-2022 WeAreFrank!

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
package nl.nn.adapterframework.configuration;

import lombok.Getter;

/**
 * Enumeration class for suppressing configuration warnings.
 * 
 * @author alisihab
 *
 */
public enum SuppressKeys {

	SQL_INJECTION_SUPPRESS_KEY("warnings.suppress.sqlInjections"),
	DEPRECATION_SUPPRESS_KEY("warnings.suppress.deprecated", true),
	DEFAULT_VALUE_SUPPRESS_KEY("warnings.suppress.defaultvalue", true),
	TRANSACTION_SUPPRESS_KEY("warnings.suppress.transaction"),
	INTEGRITY_CHECK_SUPPRESS_KEY("warnings.suppress.integrityCheck"),
	RESULT_SET_HOLDABILITY("warnings.suppress.resultSetHoldability", true),
	CONFIGURATION_VALIDATION("warnings.suppress.configurations.validation", false),
	FLOW_GENERATION_ERROR("warnings.suppress.flow.generation", true),
	MULTIPASSWORD_KEYSTORE_SUPPRESS_KEY("warnings.suppress.multiPasswordKeystore", true),
	XSLT_STREAMING_SUPRESS_KEY("warnings.suppress.xslt.streaming", true);

	private @Getter String key;
	private @Getter boolean allowGlobalSuppression = false;

	private SuppressKeys(String key) {
		this(key, false);
	}

	private SuppressKeys(String key, boolean allowGlobalSuppression) {
		this.key = key;
		this.allowGlobalSuppression = allowGlobalSuppression;
	}
}
