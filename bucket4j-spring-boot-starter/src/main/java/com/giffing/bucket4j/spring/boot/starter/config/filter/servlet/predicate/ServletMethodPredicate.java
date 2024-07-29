package com.giffing.bucket4j.spring.boot.starter.config.filter.servlet.predicate;

import com.giffing.bucket4j.spring.boot.starter.config.filter.predicate.MethodExecutePredicate;
import javax.servlet.http.HttpServletRequest;

public class ServletMethodPredicate extends MethodExecutePredicate<HttpServletRequest> {

	@Override
	public boolean test(HttpServletRequest t) {
		return testRequestMethod(t.getMethod());
	}

}
