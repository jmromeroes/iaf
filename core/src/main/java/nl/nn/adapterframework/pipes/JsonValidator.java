/*
   Copyright 2022 WeAreFrank!

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
package nl.nn.adapterframework.pipes;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.leadpony.justify.api.JsonSchema;
import org.leadpony.justify.api.JsonValidationService;
import org.leadpony.justify.api.ProblemHandler;

import jakarta.json.stream.JsonParser;
import jakarta.json.stream.JsonParsingException;
import lombok.Getter;
import nl.nn.adapterframework.configuration.ConfigurationException;
import nl.nn.adapterframework.core.PipeForward;
import nl.nn.adapterframework.core.PipeLineSession;
import nl.nn.adapterframework.core.PipeRunException;
import nl.nn.adapterframework.core.PipeStartException;
import nl.nn.adapterframework.core.Resource;
import nl.nn.adapterframework.doc.Category;
import nl.nn.adapterframework.doc.IbisDoc;
import nl.nn.adapterframework.stream.Message;
import nl.nn.adapterframework.validation.AbstractXmlValidator.ValidationResult;


/**
 * Pipe that validates the input message against a JSON Schema.
 *
 * @author Gerrit van Brakel
 */
@Category("Basic")
public class JsonValidator extends ValidatorBase {

	private @Getter String schema;
	//private @Getter String jsonSchemaVersion=null;
	private @Getter String subSchemaPrefix="/definitions/";
	private @Getter String reasonSessionKey = "failureReason";

	private JsonValidationService service = JsonValidationService.newInstance();
	private JsonSchema jsonSchema;

	@Override
	public void configure() throws ConfigurationException {
		super.configure();
		if (getSubSchemaPrefix()==null) {
			setSubSchemaPrefix("");
		}
	}

	@Override
	public void start() throws PipeStartException {
		try {
			super.start();
			jsonSchema = getJsonSchema();
		} catch (IOException e) {
			throw new PipeStartException("unable to start validator", e);
		}
	}

	@Override
	protected PipeForward validate(Message messageToValidate, PipeLineSession session, boolean responseMode, String messageRoot) throws PipeRunException {
		final List<String> problems = new LinkedList<>();
		// Problem handler which will print problems found.
		ProblemHandler handler = service.createProblemPrinter(problems::add);
		ValidationResult resultEvent;
		try {
			if (messageToValidate.isEmpty()) {
				messageToValidate = new Message("{}");
			} else {
				messageToValidate.preserve();
			}
			JsonSchema curSchema = jsonSchema;
			if (StringUtils.isEmpty(messageRoot)) {
				messageRoot = responseMode ? getResponseRoot() : getRoot();
			}
			if (StringUtils.isNotEmpty(messageRoot)) {
				log.debug("validation to messageRoot ["+messageRoot+"]");
				curSchema = jsonSchema.getSubschemaAt(getSubSchemaPrefix()+messageRoot);
				if (curSchema==null) {
					throw new PipeRunException(this, "No schema found for ["+getSubSchemaPrefix()+messageRoot+"]");
				}
			}
			// Parses the JSON instance by JsonParser
			try (JsonParser parser = service.createParser(messageToValidate.asInputStream(), curSchema, handler)) {
				while (parser.hasNext()) {
					JsonParser.Event event = parser.next();
					// Could do something useful here, like posting the event on a JsonEventHandler.
				}
				resultEvent = problems.isEmpty()? ValidationResult.VALID : ValidationResult.INVALID;
			} catch (JsonParsingException e) {
				resultEvent = ValidationResult.PARSER_ERROR;
				problems.add(e.getMessage());
			}
			if (StringUtils.isNotEmpty(getReasonSessionKey())) {
				session.put(getReasonSessionKey(), problems.toString());
			}
			return determineForward(resultEvent, session, responseMode, problems::toString);
		} catch (IOException e) {
			throw new PipeRunException(this, "cannot validate", e);
		}
	}


	protected JsonSchema getJsonSchema() throws IOException {
		String schemaName = getSchema();
		Resource schemaRes = Resource.getResource(this, schemaName);
		if (schemaRes==null) {
			throw new FileNotFoundException("Cannot find schema ["+schemaName+"]");
		}
		JsonSchema result = service.readSchema(schemaRes.openStream());
		return result;
	}

	@IbisDoc({"The JSON Schema to validate to", ""})
	public void setSchema(String schema) {
		this.schema=schema;
	}

	@IbisDoc({"Prefix to element name to find subschema in schema", "/definitions/"})
	public void setSubSchemaPrefix(String prefix) {
		subSchemaPrefix = prefix;
	}

	@IbisDoc({"If set: key of session variable to store reasons of mis-validation in", "failureReason"})
	public void setReasonSessionKey(String reasonSessionKey) {
		this.reasonSessionKey = reasonSessionKey;
	}

}