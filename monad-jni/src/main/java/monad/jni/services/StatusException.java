// Copyright 2014,2015 the original author or authors. All rights reserved.
// site: http://www.ganshane.com
package monad.jni.services;

import monad.jni.services.gen.StatusCode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author jcai
 */
public class StatusException extends Exception {
    //Code(3): not found
    private static final Pattern pattern = Pattern.compile("^Code\\(([\\d])\\)");
    private StatusCode code;

    public StatusException(String message) {
        super(message);
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            code = StatusCode.swigToEnum(Integer.parseInt(matcher.group(1)));
        }
    }

    public StatusCode getCode() {
        return code;
    }

    public boolean isNotFound() {
        return code == StatusCode.kNotFound;
    }
}
