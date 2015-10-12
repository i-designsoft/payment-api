package io.yope.config;

import java.io.IOException;
import java.util.Locale;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import io.yope.utils.ThreadLocalUtils;
import lombok.extern.slf4j.Slf4j;

@Component @Slf4j
public class LocaleFilter extends OncePerRequestFilter {
	
	@Override
	protected void initFilterBean() throws ServletException {
		super.initFilterBean();
		log.info("Locale Filter Started");
	}

	@Override
	protected void doFilterInternal(final HttpServletRequest request, 
			final HttpServletResponse response, final FilterChain chain)
			throws ServletException, IOException {
		
		final Locale locale = request.getLocale();
		LocaleContextHolder.setLocale(locale);
		ThreadLocalUtils.currentRequest.set(request);
		chain.doFilter(request, response);
	}
}
