package com.verapi.portal.handler;

import com.verapi.portal.verticle.GatewayHttpServerVerticle;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.parser.ResolverCache;
import io.vertx.ext.web.api.RequestParameter;
import io.vertx.ext.web.api.contract.impl.HTTPOperationRequestValidationHandlerImpl;
import io.vertx.ext.web.api.contract.openapi3.OpenAPI3RequestValidationHandler;
import io.vertx.ext.web.api.contract.openapi3.impl.OpenApi3Utils;
import io.vertx.ext.web.api.validation.ParameterLocation;
import io.vertx.ext.web.api.validation.ParameterType;
import io.vertx.ext.web.api.validation.ParameterTypeValidator;
import io.vertx.ext.web.api.validation.SpecFeatureNotSupportedException;
import io.vertx.ext.web.api.validation.ValidationException;
import io.vertx.ext.web.api.validation.impl.AnyOfTypeValidator;
import io.vertx.ext.web.api.validation.impl.ArrayTypeValidator;
import io.vertx.ext.web.api.validation.impl.JsonTypeValidator;
import io.vertx.ext.web.api.validation.impl.ParameterValidationRuleImpl;
import io.vertx.ext.web.api.validation.impl.RegularExpressions;
import io.vertx.ext.web.impl.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenAPI3ResponseValidationHandlerImpl extends HTTPOperationRequestValidationHandlerImpl<Operation> implements OpenAPI3RequestValidationHandler {

    private static Logger logger = LoggerFactory.getLogger(OpenAPI3ResponseValidationHandlerImpl.class);


    /*private final static ParameterTypeValidator CONTENT_TYPE_VALIDATOR = new ParameterTypeValidator() {

        @Override
        public RequestParameter isValid(String value) throws ValidationException {
            return RequestParameter.create(value);
        }

        @Override
        public RequestParameter isValidCollection(List<String> value) throws ValidationException {
            if (value.size() > 1) return RequestParameter.create(value);
            else return this.isValid(value.get(0));
        }
    };*/

    //List<Parameter> resolvedParameters;
    OpenAPI spec;
    //ResolverCache refsCache;

    /* --- Initialization functions --- */

    public OpenAPI3ResponseValidationHandlerImpl(Operation pathSpec, OpenAPI spec) {
        super(pathSpec);
        logger.trace("OpenAPI3ResponseValidationHandlerImpl constructor invoked: {} : {}", pathSpec.getOperationId(), spec.getInfo().getTitle());
        //this.resolvedParameters = resolvedParameters;
        this.spec = spec;
        //this.refsCache = refsCache;
        parseOperationSpec();
    }

    @Override
    public void parseOperationSpec() {
        logger.trace("parseOperationSpec invoked");
        // Extract from path spec parameters description
/*        if (resolvedParameters!=null) {
            for (Parameter opParameter : resolvedParameters) {
                if (opParameter.get$ref() != null)
                    opParameter = refsCache.loadRef(opParameter.get$ref(), computeRefFormat(opParameter.get$ref()), Parameter.class);
                this.parseParameter(opParameter);
            }
        }*/
        ApiResponse apiResponse = this.pathSpec.getResponses().get("200");//.getContent(); //.get("application/json").getSchema();
        if (apiResponse != null) {
            //if (body.get$ref() != null)
            //    body = refsCache.loadRef(body.get$ref(), computeRefFormat(body.get$ref()), RequestBody.class);
            this.parseApiResponse(apiResponse);
        }
    }

    /* Entry point for parse RequestBody object */
    private void parseApiResponse(ApiResponse apiResponse) {
        logger.trace("parseApiResponse invoked");
        if (apiResponse != null && apiResponse.getContent() != null) {
            for (Map.Entry<String, ? extends MediaType> mediaType : apiResponse.getContent().entrySet()) {
                if (Utils.isJsonContentType(mediaType.getKey()) && mediaType.getValue().getSchema() != null) {
                    this.setEntireBodyValidator(JsonTypeValidator.JsonTypeValidatorFactory
                            .createJsonTypeValidator(OpenApi3Utils.generateSanitizedJsonSchemaNode(mediaType.getValue().getSchema(), this.spec)));
                }
            }
            this.bodyRequired = true;//(apiResponse.getRequired() == null) ? false : apiResponse.getRequired();
        }
    }


    /* --- Type parsing functions --- */

    /* This function manage don't manage array, object, anyOf, oneOf, allOf. parseEnum is required for enum parsing
    recursion call */
    /*private ParameterTypeValidator resolveInnerSchemaPrimitiveTypeValidator(Schema schema, boolean parseEnum) {
        if (schema == null) {
            // It will never reach this
            return ParameterType.GENERIC_STRING.validationMethod();
        }
        if (parseEnum && schema.getEnum() != null && schema.getEnum().size() != 0) {
            return ParameterTypeValidator.createEnumTypeValidatorWithInnerValidator(new ArrayList(schema.getEnum()), this
                    .resolveInnerSchemaPrimitiveTypeValidator(schema, false));
        }
        switch (schema.getType()) {
            case "integer":
                if (schema.getFormat() != null && schema.getFormat().equals("int64")) {
                    return ParameterTypeValidator.createLongTypeValidator(schema.getExclusiveMaximum(), (schema.getMaximum() !=
                            null) ? schema.getMaximum().doubleValue() : null, schema.getExclusiveMinimum(), (schema.getMinimum() !=
                            null) ? schema.getMinimum().doubleValue() : null, (schema.getMultipleOf() != null) ? schema.getMultipleOf
                            ().doubleValue() : null, (schema.getDefault() != null) ? schema.getDefault().toString() : null);
                } else {
                    return ParameterTypeValidator.createIntegerTypeValidator(schema.getExclusiveMaximum(), (schema.getMaximum() !=
                            null) ? schema.getMaximum().doubleValue() : null, schema.getExclusiveMinimum(), (schema.getMinimum() !=
                            null) ? schema.getMinimum().doubleValue() : null, (schema.getMultipleOf() != null) ? schema.getMultipleOf
                            ().doubleValue() : null, (schema.getDefault() != null) ? schema.getDefault().toString() : null);
                }
            case "number":
                if (schema.getFormat() != null && schema.getFormat().equals("float"))
                    return ParameterTypeValidator.createFloatTypeValidator(schema.getExclusiveMaximum(), (schema.getMaximum() !=
                            null) ? schema.getMaximum().doubleValue() : null, schema.getExclusiveMinimum(), (schema.getMinimum() !=
                            null) ? schema.getMinimum().doubleValue() : null, (schema.getMultipleOf() != null) ? schema.getMultipleOf
                            ().doubleValue() : null, (schema.getDefault() != null) ? schema.getDefault().toString() : null);
                else
                    return ParameterTypeValidator.createDoubleTypeValidator(schema.getExclusiveMaximum(), (schema.getMaximum()
                            != null) ? schema.getMaximum().doubleValue() : null, schema.getExclusiveMinimum(), (schema.getMinimum() !=
                            null) ? schema.getMinimum().doubleValue() : null, (schema.getMultipleOf() != null) ? schema.getMultipleOf
                            ().doubleValue() : null, (schema.getDefault() != null) ? schema.getDefault().toString() : null);
            case "boolean":
                return ParameterTypeValidator.createBooleanTypeValidator(schema.getDefault());
            case "string":
                String regex = null;
                // Then resolve various string formats
                if (schema.getFormat() != null) switch (schema.getFormat()) {
                    case "binary":
                        break;
                    case "byte":
                        regex = RegularExpressions.BASE64;
                        break;
                    case "date":
                        regex = RegularExpressions.DATE;
                        break;
                    case "date-time":
                        regex = RegularExpressions.DATETIME;
                        break;
                    case "ipv4":
                        regex = RegularExpressions.IPV4;
                        break;
                    case "ipv6":
                        regex = RegularExpressions.IPV6;
                        break;
                    case "hostname":
                        regex = RegularExpressions.HOSTNAME;
                        break;
                    case "email":
                        regex = RegularExpressions.EMAIL;
                        break;
                    default:
                        throw new SpecFeatureNotSupportedException("format " + schema.getFormat() + " not supported");
                }
                return ParameterTypeValidator.createStringTypeValidator((regex != null) ? regex : schema.getPattern(), schema
                        .getMinLength(), schema.getMaxLength(), schema.getDefault());

        }
        return ParameterType.GENERIC_STRING.validationMethod();
    }*/

    /* This function is an overlay for below function */
/*    private void resolveObjectTypeFields(ObjectTypeValidator validator, Schema schema) {
        Map<String, OpenApi3Utils.ObjectField> parameters = OpenApi3Utils.solveObjectParameters(schema);
        for (Map.Entry<String, OpenApi3Utils.ObjectField> entry : parameters.entrySet()) {
            validator.addField(entry.getKey(), this.resolveInnerSchemaPrimitiveTypeValidator(entry.getValue().getSchema(), true), entry.getValue().isRequired());
        }
    }
*/
    /* This function resolve all type validators of anyOf or oneOf type (schema) arrays. It calls the function below */
/*
    private List<ParameterTypeValidator> resolveTypeValidatorsForAnyOfOneOf(List<Schema> schemas, Parameter parent) {
        List<ParameterTypeValidator> result = new ArrayList<>();
        for (Schema schema : schemas) {
            result.add(this.resolveAnyOfOneOfTypeValidator(schema, parent));
        }
        return result;
    }
*/

    /* This function manage a single schema of anyOf or oneOf type (schema) arrays */
/*
    private ParameterTypeValidator resolveAnyOfOneOfTypeValidator(Schema schema, Parameter parent) {
        if (schema.getType().equals("array"))
            return ArrayTypeValidator.ArrayTypeValidatorFactory.createArrayTypeValidator(this
                    .resolveInnerSchemaPrimitiveTypeValidator(schema, true), OpenApi3Utils.resolveStyle(parent), parent.getExplode
                    (), schema.getMaxItems(), schema.getMinItems());
        else if (schema.getType().equals("object")) {
            ObjectTypeValidator objectTypeValidator = ObjectTypeValidator.ObjectTypeValidatorFactory
                    .createObjectTypeValidator(OpenApi3Utils.resolveStyle(parent), parent.getExplode());
            resolveObjectTypeFields(objectTypeValidator, schema);
            return objectTypeValidator;
        }
        return this.resolveInnerSchemaPrimitiveTypeValidator(schema, true);
    }
*/

    /* This function check if parameter is of type oneOf, allOf, anyOf and return required type validators. It's
    detached from below function to call it from "magic" workarounds functions */
/*
    private ParameterTypeValidator resolveAnyOfOneOfTypeValidator(Parameter parameter) {
        ComposedSchema composedSchema;
        if (parameter.getSchema() instanceof ComposedSchema) composedSchema = (ComposedSchema) parameter.getSchema();
        else return null;

        if (OpenApi3Utils.isAnyOfSchema(composedSchema)) {
            return new AnyOfTypeValidator(this.resolveTypeValidatorsForAnyOfOneOf(new ArrayList<>(composedSchema.getAnyOf
                    ()), parameter));
        } else if (OpenApi3Utils.isOneOfSchema(composedSchema)) {
            return new OneOfTypeValidator(this.resolveTypeValidatorsForAnyOfOneOf(new ArrayList<>(composedSchema.getOneOf
                    ()), parameter));
        } else return null;
    }
*/

    /* Entry point for resolve type validators */
/*
    private ParameterTypeValidator resolveTypeValidator(Parameter parameter) {
        ParameterTypeValidator candidate = resolveAnyOfOneOfTypeValidator(parameter);
        if (candidate != null) return candidate;
        else if (OpenApi3Utils.isParameterArrayType(parameter)) {
            ArraySchema arraySchema = (ArraySchema) parameter.getSchema();
            return ArrayTypeValidator.ArrayTypeValidatorFactory.createArrayTypeValidator(this
                    .resolveInnerSchemaPrimitiveTypeValidator(arraySchema.getItems(), true), OpenApi3Utils
                    .resolveStyle(parameter), parameter.getExplode(), parameter.getSchema().getMaxItems(), parameter.getSchema()
                    .getMinItems());
        } else if (OpenApi3Utils.isParameterObjectOrAllOfType(parameter)) {
            ObjectTypeValidator objectTypeValidator = ObjectTypeValidator.ObjectTypeValidatorFactory
                    .createObjectTypeValidator(OpenApi3Utils.resolveStyle(parameter), parameter.getExplode());
            resolveObjectTypeFields(objectTypeValidator, parameter.getSchema());
            return objectTypeValidator;
        }
        return this.resolveInnerSchemaPrimitiveTypeValidator(parameter.getSchema(), true);
    }
*/

    /* --- "magic" functions for workarounds (watch below for more info) --- */

    // content field can support every mime type. I will use a default type validator for every content type, except
    // application/json, that i can validate with JsonTypeValidator
    // If content has multiple media types, I use anyOfTypeValidator to support every content type
    /*private void handleContent(Parameter parameter) {
        Content contents = parameter.getContent();
        ParameterLocation location = resolveLocation(parameter.getIn());
        List<MediaType> jsonsContents = OpenApi3Utils.extractTypesFromMediaTypesMap(contents, Utils::isJsonContentType);
        if (jsonsContents.size() == 1) {
            this.addRule(ParameterValidationRuleImpl.ParameterValidationRuleFactory
                    .createValidationRuleWithCustomTypeValidator(parameter.getName(), JsonTypeValidator.JsonTypeValidatorFactory
                                    .createJsonTypeValidator(OpenApi3Utils.generateSanitizedJsonSchemaNode(jsonsContents.get(0).getSchema(), this.spec)),
                            !parameter.getRequired(), OpenApi3Utils.resolveAllowEmptyValue(parameter), location), location);
        } else if (contents.size() > 1 && jsonsContents.size() >= 1) {
            // Mount anyOf
            List<ParameterTypeValidator> validators =
                    jsonsContents.stream().map(e -> JsonTypeValidator.JsonTypeValidatorFactory
                            .createJsonTypeValidator(OpenApi3Utils.generateSanitizedJsonSchemaNode(e.getSchema(), this.spec))).collect(Collectors.toList());
            validators.add(CONTENT_TYPE_VALIDATOR);
            AnyOfTypeValidator validator = new AnyOfTypeValidator(validators);
            this.addRule(ParameterValidationRuleImpl.ParameterValidationRuleFactory
                    .createValidationRuleWithCustomTypeValidator(parameter.getName(), validator, !parameter.getRequired(), OpenApi3Utils.resolveAllowEmptyValue(parameter)
                            , location), location);
        } else {
            this.addRule(ParameterValidationRuleImpl.ParameterValidationRuleFactory
                    .createValidationRuleWithCustomTypeValidator(parameter.getName(), CONTENT_TYPE_VALIDATOR, !parameter
                                    .getRequired(), OpenApi3Utils.resolveAllowEmptyValue(parameter),
                            location), location);
        }
    }*/

    /*private void magicParameterExplodedMatrixArray(Parameter parameter) {
        ParameterTypeValidator validator = ArrayTypeValidator.ArrayTypeValidatorFactory.createArrayTypeValidator(this
                .resolveInnerSchemaPrimitiveTypeValidator(((ArraySchema) parameter.getSchema()).getItems(), true), "matrix_exploded_array", true, parameter.getSchema().getMaxItems(), parameter.getSchema()
                .getMinItems());

        this.addPathParamRule(ParameterValidationRuleImpl.ParameterValidationRuleFactory
                .createValidationRuleWithCustomTypeValidator(parameter.getName(), validator, parameter.getRequired(), false, ParameterLocation.PATH));
    }*/

/*
    private void magicParameterExplodedStyleSimpleTypeObject(Parameter parameter) {
        ObjectTypeValidator objectTypeValidator = ObjectTypeValidator.ObjectTypeValidatorFactory
                .createObjectTypeValidator(ContainerSerializationStyle.simple_exploded_object, false);
        this.resolveObjectTypeFields(objectTypeValidator, parameter.getSchema());
        if (parameter.getIn().equals("path")) {
            this.addPathParamRule(ParameterValidationRuleImpl.ParameterValidationRuleFactory
                    .createValidationRuleWithCustomTypeValidator(parameter.getName(), objectTypeValidator, !OpenApi3Utils
                            .isRequiredParam(parameter), OpenApi3Utils.resolveAllowEmptyValue(parameter), ParameterLocation.PATH));
        } else if (parameter.getIn().equals("header")) {
            this.addHeaderParamRule(ParameterValidationRuleImpl.ParameterValidationRuleFactory
                    .createValidationRuleWithCustomTypeValidator(parameter.getName(), objectTypeValidator, !OpenApi3Utils
                            .isRequiredParam(parameter), OpenApi3Utils.resolveAllowEmptyValue(parameter), ParameterLocation.HEADER));
        } else {
            throw new SpecFeatureNotSupportedException("combination of style, type and location (in) of parameter fields "
                    + "not supported for parameter " + parameter.getName());
        }
    }
*/


    /* This function check if a parameter has some particular configurations and run the needed flow to adapt it to
    vertx-web validation framework
     * Included not supported throws:
     * - allowReserved field (it will never be supported)
     * - cookie parameter with explode: true
     * Included workarounds (handled in "magic" functions):
     * - content
     * - exploded: true & style: form & type: object or allOf -> magicParameterExplodedStyleFormTypeObject
     * - exploded: true & style: simple & type: object or allOf -> magicParameterExplodedStyleSimpleTypeObject
     * - exploded: true & style: deepObject & type: object or allOf -> magicParameterExplodedStyleDeepObjectTypeObject
     * */
//    private boolean checkSupportedAndNeedWorkaround(Parameter parameter) {
//        if (Boolean.TRUE == parameter.getAllowReserved()) {
//            throw new SpecFeatureNotSupportedException("allowReserved field not supported!");
//        } else if (parameter.getContent() != null && parameter.getContent().size() != 0) {
//            handleContent(parameter);
//            return true;
//        } else
// From this moment only astonishing magic happens
// if (parameter.getExplode()) {
//            boolean isObject = OpenApi3Utils.isParameterObjectOrAllOfType(parameter);
//            String style = OpenApi3Utils.resolveStyle(parameter);
//            if (OpenApi3Utils.isParameterArrayType(parameter) && "matrix".equals(style)) {
//                this.magicParameterExplodedMatrixArray(parameter);
//                return true;
//            }
//            if (isObject && "simple".equals(style)) {
//                this.magicParameterExplodedStyleSimpleTypeObject(parameter);
//                return true;
//            } else {
//                return false;
//            }
//        }
//        return false;
//    }

    /* Function to resolve ParameterLocation from in string */
/*    private ParameterLocation resolveLocation(String in) {
        switch (in) {
            case "header":
                return ParameterLocation.HEADER;
            case "query":
                return ParameterLocation.QUERY;
            case "cookie":
                return ParameterLocation.COOKIE;
            case "path":
                return ParameterLocation.PATH;
            default:
                throw new SpecFeatureNotSupportedException("in field wrong or not supported");
        }
    }*/

    /* Entry point for parse Parameter object */
/*
    private void parseParameter(Parameter parameter) {
        if (!checkSupportedAndNeedWorkaround(parameter)) {
            ParameterLocation location = resolveLocation(parameter.getIn());
            this.addRule(ParameterValidationRuleImpl.ParameterValidationRuleFactory
                    .createValidationRuleWithCustomTypeValidator(parameter.getName(), this.resolveTypeValidator(parameter),
                            !parameter.getRequired(), OpenApi3Utils.resolveAllowEmptyValue(parameter), location), location);
        }
    }
*/

    /* --- Request body functions. All functions below are used to parse RequestBody object --- */

    /* This function resolve types for x-www-form-urlencoded. It sets all Collections styles to "csv" */
/*    private ParameterTypeValidator resolveSchemaTypeValidatorFormEncoded(Schema schema) {
        if (schema.getType().equals("array"))
            return ArrayTypeValidator.ArrayTypeValidatorFactory.createArrayTypeValidator(this
                            .resolveInnerSchemaPrimitiveTypeValidator(((ArraySchema) schema).getItems(), true), "csv", false, schema.getMaxItems(),
                    schema.getMinItems());
        else if (OpenApi3Utils.isSchemaObjectOrAllOfType(schema)) {
            ObjectTypeValidator objectTypeValidator = ObjectTypeValidator.ObjectTypeValidatorFactory
                    .createObjectTypeValidator("csv", false);
            resolveObjectTypeFields(objectTypeValidator, schema);
            return objectTypeValidator;
        }
        return this.resolveInnerSchemaPrimitiveTypeValidator(schema, true);
    }
*/
    /* This function resolves default content types of multipart parameters */
/*    private String resolveDefaultContentTypeRegex(Schema schema) {
        if (OpenApi3Utils.isSchemaObjectOrAllOfType(schema))
            return "\\Qapplication/json\\E|.*\\/.*\\+json"; // Regex for json content type

        if (schema.getType() != null) {
            if (schema.getType().equals("string") && schema.getFormat() != null && (schema.getFormat().equals
                    ("binary") || schema.getFormat().equals("base64")))
                return Pattern.quote("application/octet-stream");
            else if (schema.getType().equals("array"))
                return this.resolveDefaultContentTypeRegex(((ArraySchema) schema).getItems());
            else return Pattern.quote("text/plain");
        }

        throw new SpecFeatureNotSupportedException("Unable to find default content type for multipart parameter. Use " +
                "encoding field");
    }
*/

}
