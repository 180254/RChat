/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 * ------------------------------------------------------------
 *
 * Modification by 180254 under Apache License 2.0.
 * http://www.apache.org/licenses/LICENSE-2.0
 */
package pl.nn44.xmlrpc;

import org.apache.xmlrpc.common.TypeConverter;
import org.apache.xmlrpc.common.TypeConverterFactory;
import org.apache.xmlrpc.common.XmlRpcInvocationException;

import java.lang.reflect.Proxy;
import java.lang.reflect.UndeclaredThrowableException;

/**
 * <pre>
 * org.apache.xmlrpc.client.util.ClientFactory fix.
 * - toString(), equals(), hashCode() are not handled properly by original ClientFactory.
 * proxy doc: https://docs.oracle.com/javase/8/docs/technotes/guides/reflection/proxy.html
 * </pre>
 */
public class ClientFactoryFix {

    public static Object newInstance(
            ClassLoader classLoader,
            Class<?> clazz,
            String serverUrl,
            ClientExecutor clientExecutor,
            TypeConverterFactory typeConverterFactory
    ) {

        return Proxy.newProxyInstance(
                classLoader,
                new Class[]{clazz},
                (proxy, method, args) -> {

                    /*
                    org.apache.xmlrpc.client.util.ClientFactory implementation:
                    if (isObjectMethodLocal()  &&  pMethod.getDeclaringClass().equals(Object.class)) {
                        return pMethod.invoke(pProxy, pArgs);
                    }
                    */
                    if (method.getDeclaringClass().equals(Object.class)) {
                        switch (method.getName()) {
                            case "toString":
                                return "XmlRpcProxy[" + serverUrl + "]";
                            case "equals":
                                return proxy == args[0];
                            case "hashCode":
                                return System.identityHashCode(proxy);
                            default:
                                throw new AssertionError("unexpected method dispatched: " + method);
                        }
                    }

                    String remoteName = clazz.getSimpleName();
                    String methodName = remoteName + "." + method.getName();

                    Object result;
                    try {
                        result = clientExecutor.execute(methodName, args);

                    } catch (XmlRpcInvocationException e) {
                        Throwable t = e.linkedException;
                        if (t instanceof RuntimeException) {
                            throw t;
                        }

                        Class<?>[] exceptionTypes = method.getExceptionTypes();
                        for (Class<?> eClass : exceptionTypes) {
                            if (eClass.isAssignableFrom(t.getClass())) {
                                throw t;
                            }
                        }

                        throw new UndeclaredThrowableException(t);
                    }

                    TypeConverter typeConverter = typeConverterFactory.getTypeConverter(method.getReturnType());
                    return typeConverter.convert(result);
                }
        );
    }
}
