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
package nl.nn.adapterframework.util;

import java.sql.JDBCType;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import nl.nn.adapterframework.core.SenderException;
import nl.nn.adapterframework.jdbc.dbms.IDbmsSupport;
import nl.nn.adapterframework.stream.MessageOutputStream;
import nl.nn.adapterframework.stream.StreamingException;
import nl.nn.adapterframework.stream.document.ArrayBuilder;
import nl.nn.adapterframework.stream.document.DocumentBuilderFactory;
import nl.nn.adapterframework.stream.document.DocumentFormat;
import nl.nn.adapterframework.stream.document.INodeBuilder;
import nl.nn.adapterframework.stream.document.ObjectBuilder;

public class DB2DocumentWriter {
	protected static Logger log = LogUtil.getLogger(DB2DocumentWriter.class);

	private String docname = "result";
	private String recordname = "rowset";
	private String nullValue = "";
	private boolean trimSpaces=true;
	private boolean decompressBlobs=false;
	private boolean getBlobSmart=false;
	private String blobCharset = Misc.DEFAULT_INPUT_STREAM_ENCODING;
	private static boolean convertFieldnamesToUppercase = AppConstants.getInstance().getBoolean("jdbc.convertFieldnamesToUppercase", false);

	public static String getFieldType (int type) {
		return JDBCType.valueOf(type).getName();
	}

	/**
	 * Retrieve the Resultset as a well-formed XML string
	 */
	public String getXML(IDbmsSupport dbmsSupport, ResultSet rs) {
		return getXML(dbmsSupport, rs, Integer.MAX_VALUE);
	}

	/**
	 * Retrieve the Resultset as a well-formed XML string
	 */
	public String getXML(IDbmsSupport dbmsSupport, ResultSet rs, int maxlength) {
		return getXML(dbmsSupport, rs, maxlength, true);
	}

	public String getXML(IDbmsSupport dbmsSupport, ResultSet rs, int maxlength, boolean includeFieldDefinition) {
		try {
			ObjectBuilder documentBuilder = DocumentBuilderFactory.startObjectDocument(DocumentFormat.XML, docname);
			try {
				writeDocument(dbmsSupport, rs, maxlength, includeFieldDefinition, documentBuilder);
			} finally {
				documentBuilder.close();
			}
			return documentBuilder.toString();
		} catch (SAXException e) {
			log.warn("cannot convert ResultSet to XML", e);
			return "<error>"+XmlUtils.encodeCharsAndReplaceNonValidXmlCharacters(e.getMessage())+"</error>";
		}
	}

	public void writeDocument(DocumentFormat format, IDbmsSupport dbmsSupport, ResultSet rs, int maxlength, boolean includeFieldDefinition, MessageOutputStream target, boolean prettyPrint) throws StreamingException, SAXException {
		try (ObjectBuilder documentBuilder = DocumentBuilderFactory.startObjectDocument(format, docname, target, prettyPrint)) {
			writeDocument(dbmsSupport, rs, maxlength, includeFieldDefinition, documentBuilder);
		}
	}

	public void writeDocument(IDbmsSupport dbmsSupport, ResultSet rs, int maxlength, boolean includeFieldDefinition, ObjectBuilder documentBuilder) throws SAXException {

		if (null == rs) {
			return;
		}
		if (maxlength < 0) {
			maxlength = Integer.MAX_VALUE;
		}
		Statement stmt=null;
		try {
			stmt = rs.getStatement();
			if (stmt!=null) {
				JdbcUtil.warningsToDocument(stmt.getWarnings(), documentBuilder);
			}
		} catch (SQLException e1) {
			log.warn("exception obtaining statement warnings", e1);
		}
		int rowCounter=0;
		try {
			ResultSetMetaData rsmeta = rs.getMetaData();
			if (includeFieldDefinition) {
				addFieldDefinitions(documentBuilder, rsmeta);
			}

			//----------------------------------------
			// Process result rows
			//----------------------------------------

			try (ArrayBuilder rows = documentBuilder.addArrayField(recordname,"row")) {
				while (rs.next() && rowCounter < maxlength) {
					writeRow(rows, dbmsSupport, rs,rsmeta,getBlobCharset(),decompressBlobs,nullValue,trimSpaces,getBlobSmart);
					rowCounter++;
				}
			}
		} catch (Exception e) {
			log.error("Error occured at row [" + rowCounter+"]", e);
		}
	}

//	public static String getFieldDefinitions(ResultSet rs) throws SAXException, SQLException {
//		XmlWriter writer = new XmlWriter();
//		addFieldDefinitions(rs, writer);
//		return writer.toString();
//	}
//	public static void addFieldDefinitions(ResultSet rs, ContentHandler handler) throws SAXException, SQLException {
//		ResultSetMetaData rsmeta = rs.getMetaData();
//		try (SaxElementBuilder fields = new SaxElementBuilder("fielddefinition", handler)) {
//			addFieldDefinitionsToContainer(fields, rsmeta);
//		}
//	}
	public static void addFieldDefinitions(ObjectBuilder documentBuilder, ResultSetMetaData rsmeta) throws SAXException, SQLException {
		try (ArrayBuilder fields = documentBuilder.addArrayField("fielddefinition","field")) {
			addFieldDefinitionsToContainer(fields, rsmeta);
		}
	}

	private static void addFieldDefinitionsToContainer(ArrayBuilder fields, ResultSetMetaData rsmeta) throws SAXException, SQLException {
		int nfields = rsmeta.getColumnCount();

		for (int j = 1; j <= nfields; j++) {
			try (INodeBuilder nodeBuilder=fields.addElement()) {
				try (ObjectBuilder field=nodeBuilder.startObject()) {

					String columnName = rsmeta.getColumnName(j);
					if(convertFieldnamesToUppercase) {
						columnName = columnName.toUpperCase();
					}
					field.addAttribute("name", columnName);

					//Not every JDBC implementation implements these attributes!
					try {
						field.addAttribute("type", getFieldType(rsmeta.getColumnType(j)));
					} catch (SQLException e) {
						log.debug("Could not determine columnType",e);
					}
					try {
						field.addAttribute("columnDisplaySize", rsmeta.getColumnDisplaySize(j));
					} catch (SQLException e) {
						log.debug("Could not determine columnDisplaySize",e);
					}
					try {
						field.addAttribute("precision", rsmeta.getPrecision(j));
					} catch (SQLException e) {
						log.warn("Could not determine precision",e);
					} catch (NumberFormatException e2) {
						if (log.isDebugEnabled()) log.debug("Could not determine precision: "+e2.getMessage());
					}
					try {
						field.addAttribute("scale", rsmeta.getScale(j));
					} catch (SQLException e) {
						log.debug("Could not determine scale",e);
					}
					try {
						field.addAttribute("isCurrency", rsmeta.isCurrency(j));
					} catch (SQLException e) {
						log.debug("Could not determine isCurrency",e);
					}
					try {
						String columnTypeName = rsmeta.getColumnTypeName(j);
						if(convertFieldnamesToUppercase)
							columnTypeName = columnTypeName.toUpperCase();
						field.addAttribute("columnTypeName", columnTypeName);
					} catch (SQLException e) {
						log.debug("Could not determine columnTypeName",e);
					}
					try {
						field.addAttribute("columnClassName", rsmeta.getColumnClassName(j));
					} catch (SQLException e) {
						log.debug("Could not determine columnClassName",e);
					}
				}
			}
		}
	}


//	public static String getRowXml(IDbmsSupport dbmsSupport, ResultSet rs, int rowNumber, ResultSetMetaData rsmeta, String blobCharset, boolean decompressBlobs, String nullValue, boolean trimSpaces, boolean getBlobSmart) throws SenderException, SQLException, SAXException {
//		SaxElementBuilder parent = new SaxElementBuilder();
//		getRowXml(parent, dbmsSupport, rs, rowNumber, rsmeta, blobCharset, decompressBlobs, nullValue, trimSpaces, getBlobSmart);
//		return parent.toString();
//	}

	public static void writeRow(ArrayBuilder rows, IDbmsSupport dbmsSupport, ResultSet rs, ResultSetMetaData rsmeta, String blobCharset, boolean decompressBlobs, String nullValue, boolean trimSpaces, boolean getBlobSmart) throws SenderException, SQLException, SAXException {
		try (INodeBuilder nodeBuilder = rows.addElement()) {
			try (ObjectBuilder row=nodeBuilder.startObject()) {
				for (int i = 1; i <= rsmeta.getColumnCount(); i++) {
					String columnName = "" + rsmeta.getColumnName(i);
					if(convertFieldnamesToUppercase) {
						columnName = columnName.toUpperCase();
					}
					try {
						String value = JdbcUtil.getValue(dbmsSupport, rs, i, rsmeta, blobCharset, decompressBlobs, nullValue, trimSpaces, getBlobSmart, false);
						if (rs.wasNull()) {
							row.add(columnName, null);
						} else {
							if (JdbcUtil.isNumeric(rsmeta.getColumnType(i))) {
								row.addNumber(columnName, value);
							} else {
								row.add(columnName, value);
							}
						}
					} catch (Exception e) {
						throw new SenderException("error getting fieldvalue column ["+i+"] fieldType ["+getFieldType(rsmeta.getColumnType(i))+ "]", e);
					}
				}
				//JdbcUtil.warningsToXml(rs.getWarnings(), row);
			}
		}
	}


	public void setDocumentName(String s) {
		docname = s;
	}

	public void setRecordName(String s) {
		recordname = s;
	}

	/**
	 * Set the presentation of a <code>Null</code> value
	 */
	public void setNullValue(String s) {
		nullValue = s;
	}

	/**
	 * Get the presentation of a <code>Null</code> value
	 */
	public String getNullValue() {
		return nullValue;
	}

	public void setTrimSpaces(boolean b) {
		trimSpaces = b;
	}
	public boolean isTrimSpaces() {
		return trimSpaces;
	}

	public void setDecompressBlobs(boolean b) {
		decompressBlobs = b;
	}
	public boolean isDecompressBlobs() {
		return decompressBlobs;
	}

	public void setGetBlobSmart(boolean b) {
		getBlobSmart = b;
	}
	public boolean isGetBlobSmart() {
		return getBlobSmart;
	}

	public String getBlobCharset() {
		return blobCharset;
	}

	public void setBlobCharset(String string) {
		blobCharset = string;
	}

}
