package cn.bhbapp.web.core.doc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.SpringProperties;
import org.springframework.core.env.Environment;
import org.springframework.util.AntPathMatcher;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;

import springfox.documentation.RequestHandler;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.service.Parameter;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.DocumentationContextBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;
import springfox.documentation.swagger2.web.Swagger2Controller;

/**
 * API Doc Configuration
 * 
 * @author onsoul
 * @Date Apr 17, 2019 11:21:15 AM
 */
@Configuration
@Lazy
@EnableSwagger2
@ConditionalOnClass(Swagger2Controller.class)
@ConditionalOnProperty(prefix = "spring.base.web.doc", name = "swagger-enable", havingValue = "true")
public class SwaggerAutoConfiguration implements BeanFactoryPostProcessor {
	private final AntPathMatcher pathMatcher = new AntPathMatcher();
	private final Logger log = LoggerFactory.getLogger(SwaggerAutoConfiguration.class);

	public SwaggerAutoConfiguration() {
		log.debug("create bean: {}", SwaggerAutoConfiguration.class.getName());
	}

	@Bean("api-starter")
	public Docket api() {
		Docket docket = new Docket(DocumentationType.SPRING_WEB);
		docket.groupName("API");
		docket.apiInfo(apiInfo());
		docket.select().apis(matchPath("/?.?/api/**")).build();
		return docket;
	}

	@Bean("console-starter")
	public Docket console() {
		Docket docket = new Docket(DocumentationType.SPRING_WEB);
		docket.groupName("CONSOLE");
		docket.apiInfo(apiInfo());
		List<Parameter> parameters = new ArrayList<>(2);
		// Collections.addAll(parameters, sessionParameter(), appParameter());
		docket.globalOperationParameters(parameters);
		docket.select().apis(matchPath("/console/**")).build();
		return docket;
	}

	/**
	 * 其他
	 * 
	 * @param selector
	 */
	public Docket other(com.google.common.base.Predicate<RequestHandler> selector) {
		Docket docket = new Docket(DocumentationType.SPRING_WEB);
		docket.groupName("其他(other)");
		docket.apiInfo(apiInfo());
		List<Parameter> parameters = new ArrayList<>(2);
		Collections.addAll(parameters);
		docket.globalOperationParameters(parameters);
		docket.select().apis(selector).build();
		return docket;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		Map<String, Docket> beansOfType = beanFactory.getBeansOfType(Docket.class);
		List<com.google.common.base.Predicate<RequestHandler>> list = beansOfType.values().parallelStream()
				.map(docket -> {
					DocumentationContextBuilder db = new DocumentationContextBuilder(docket.getDocumentationType());
					return new com.google.common.base.Predicate<RequestHandler>() {
						@Override
						public boolean apply(RequestHandler input) {
							return !docket.configure(db).getApiSelector().getRequestHandlerSelector().apply(input);
						}
					};
				}).collect(Collectors.toList());
		beanFactory.registerSingleton("other-starter", other(Predicates.and(list)));
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder().title("Base API 文档").description("文档详细见下拉列表")
				.contact(new Contact("Big Head BRO'S", "", "onsoul@qq.com")).version("1.0.0").build();
	}

	/**
	 * private Parameter appParameter() { ParameterBuilder appToken = new
	 * ParameterBuilder(); //appToken.name(Header.).description("应用令牌").modelRef(new
	 * ModelRef("string")).parameterType("header") // .required(false).build();
	 * return appToken.build(); }
	 **/

	@SuppressWarnings("Guava")
	private Predicate<RequestHandler> matchPath(String... patterns) {
		return requestHandler -> {
			return Arrays.stream(patterns).anyMatch(pattern -> {
				return Objects.requireNonNull(requestHandler).getPatternsCondition().getPatterns().parallelStream()
						.anyMatch(requestMapping -> pathMatcher.match(pattern, requestMapping));
			});
		};
	}
}
