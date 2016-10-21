package pl.nn44.xmlrpc;

import org.apache.ws.commons.util.NamespaceContextImpl;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.common.TypeFactoryImpl;
import org.apache.xmlrpc.common.XmlRpcController;
import org.apache.xmlrpc.common.XmlRpcStreamConfig;
import org.apache.xmlrpc.parser.MapParser;
import org.apache.xmlrpc.parser.TypeParser;
import org.apache.xmlrpc.serializer.MapSerializer;
import org.apache.xmlrpc.serializer.TypeSerializer;
import org.apache.xmlrpc.serializer.TypeSerializerImpl;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class AnyTypeFactory extends TypeFactoryImpl {

    protected static final Set<Class<?>> BASIC_CLASSES = new HashSet<>();

    static {
        BASIC_CLASSES.add(Integer.class);
        BASIC_CLASSES.add(int.class);
        BASIC_CLASSES.add(Boolean.class);
        BASIC_CLASSES.add(boolean.class);
        BASIC_CLASSES.add(String.class);
        BASIC_CLASSES.add(Double.class);
        BASIC_CLASSES.add(double.class);
        BASIC_CLASSES.add(Date.class);
        BASIC_CLASSES.add(byte[].class);
        BASIC_CLASSES.add(Map.class);
        BASIC_CLASSES.add(Object[].class);
        BASIC_CLASSES.add(List.class);
    }

    protected boolean isBasicClass(Class<?> clazz) {
        return BASIC_CLASSES.stream().anyMatch(bs -> bs.isAssignableFrom(clazz));
    }

    public AnyTypeFactory(XmlRpcController pController) {
        super(pController);
    }

    @Override
    public TypeSerializer getSerializer(XmlRpcStreamConfig pConfig,
                                        Object pObject)
            throws SAXException {

        if (pObject == null || !isBasicClass(pObject.getClass())) {
            TypeSerializer mapSerializer = new MapSerializer(this, pConfig);

            return new TypeSerializerImpl() {
                @Override
                public void write(ContentHandler pHandler, Object pObject) throws SAXException {
                    try {
                        mapSerializer.write(pHandler, objectToMap(pObject));
                    } catch (IllegalAccessException e) {
                        throw new SAXException(e);
                    }
                }
            };

        } else {
            return super.getSerializer(pConfig, pObject);
        }
    }

    @Override
    public TypeParser getParser(XmlRpcStreamConfig pConfig,
                                NamespaceContextImpl pContext,
                                String pURI,
                                String pLocalName) {

        if (pLocalName.equals("struct")) {
            return new MapParser(pConfig, pContext, this) {

                @Override
                public Object getResult() throws XmlRpcException {
                    try {
                        // noinspection unchecked
                        Map<String, Object> result = (Map<String, Object>) super.getResult();

                        if (result != null && result.containsKey("__class__")) {
                            return mapToObject(result);
                        } else {
                            return result;
                        }

                    } catch (IllegalAccessException |
                            InstantiationException |
                            ClassNotFoundException |
                            NoSuchFieldException e) {
                        throw new XmlRpcException(e.getMessage(), e);
                    }

                }
            };

        } else {
            return super.getParser(pConfig, pContext, pURI, pLocalName);
        }
    }

    protected Map<String, Object> objectToMap(Object object)
            throws IllegalAccessException {

        Map<String, Object> map = new HashMap<>();

        if (object == null) {
            map.put("__class__", "null");
            return map;
        }

        map.put("__class__", object.getClass().getName());

        for (Field field : object.getClass().getDeclaredFields()) {
            field.setAccessible(true);

            String key = field.getName();
            Object value = field.get(object);

            if (field.getName().equals("serialVersionUID")) {
                // long is not supported
                continue;
            }

            if (value != null && isBasicClass(value.getClass())) {
                map.put(key, value);
            } else {
                map.put(key, objectToMap(value));
            }
        }

        return map;
    }

    protected Object mapToObject(Map<String, Object> map)
            throws
            ClassNotFoundException,
            IllegalAccessException,
            InstantiationException,
            NoSuchFieldException {

        if (map.get("__class__").equals("null")) {
            return null;
        }

        Class<?> clazz = Class.forName(map.get("__class__").toString());
        Object instance = clazz.newInstance();

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().equals("__class__")) {
                continue;
            }

            Field field = clazz.getDeclaredField(entry.getKey());
            field.setAccessible(true);

            Field modifiers = Field.class.getDeclaredField("modifiers");
            modifiers.setAccessible(true);
            modifiers.setInt(field, field.getModifiers() & ~Modifier.FINAL);

            Object value = entry.getValue();

            if (value != null && value.getClass().isAssignableFrom(Map.class)) {
                field.set(instance, mapToObject(map));
            } else {
                field.set(instance, value);
            }
        }

        return instance;
    }
}
