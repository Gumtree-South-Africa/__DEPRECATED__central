package com.ecg.messagebox.util;

import com.fasterxml.jackson.databind.introspect.AnnotatedMethod;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.google.common.base.Optional;
import io.swagger.annotations.ApiModelProperty;
import springfox.documentation.builders.ModelPropertyBuilder;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.schema.ModelPropertyBuilderPlugin;
import springfox.documentation.spi.schema.contexts.ModelPropertyContext;

/**
 * Hide 'allowEmptyValue' when field annotated with @ApiModelProperty (no officially supported)
 */
public class SwaggerCustomModelPropertyBuilderPlugin implements ModelPropertyBuilderPlugin {

    @Override
    public boolean supports(final DocumentationType type) {
        return true;
    }

    @Override
    public void apply(final ModelPropertyContext context) {
        ModelPropertyBuilder builder = context.getBuilder();

        Optional<BeanPropertyDefinition> beanPropDef = context.getBeanPropertyDefinition();
        if (beanPropDef.isPresent()) {
            BeanPropertyDefinition beanDef = beanPropDef.get();
            AnnotatedMethod method = beanDef.getGetter();
            if (method == null) {
                return;
            }

            final ApiModelProperty apiModelProperty = method.getAnnotation(ApiModelProperty.class);
            if (apiModelProperty == null) {
                return;
            }

            builder.allowEmptyValue(null);
        }
    }
}