/**
 * <pre>
 *               AXE-180254
 *  Apache XML-RPC EXTENSIONS by 180254.
 *
 *               WHAT IS IT?
 * "Extension pack" for the Apache XML-RPC implementation.
 *
 * This package is to provide support for:
 * - complex types,
 *   complex = without direct support in XML-RPC specification,
 * - transporting business exceptions.
 *
 * The following assumptions were taken into account:
 * - without any extension, only xml-rpc standard tags,
 * - without to-byte-serialization,
 * - without apache-xml-rpc-enabledForExceptions,
 * - without apache-xml-rpc-enabledForExtensions.
 *
 *               COMPLEX TYPES
 *
 * Class: AnyTypeFactory
 *
 * Now the following types are also supported:
 * - class with fields of supported types,
 * - null,
 * - enum.
 *
 * How it is done?
 * - classes are served as map:
 *   entry=("__class__", "some.java.class.full.name")
 *   for(f in object.fields): entry=(f.name, f.value)
 *
 * - nulls are served as map:
 *   entry=("__class__", "null")
 *
 * - enums are served as map:
 *   entry=("__class__", "enum")
 *   entry=("type", "some.java.class.full.name")
 *   entry=("name", enum_key.name())
 *
 * - arrays are served as map:
 *   entry=("__class__", "array")
 *   entry=("type", "some.java.class.full.name")
 *   entry=("values", struct_with_values)
 *
 *               TRANSPORT EXCEPTION
 *
 * Class (client-side): AnyXmlRpcServer
 * Class (server-side): AnyXmlRpcTransport
 *
 * How it is done?
 * Programmer must provide two fault mappers:
 *
 * - FaultMapper (Function&lt;Throwable, XmlRpcException&gt;)
 *   It converts Throwable into XmlRpcException,
 *   or returns null conversion if not supported.
 *
 * - FaultRevMapper (Function&lt;XmlRpcException, Throwable&gt;)
 *   It converts XmlRpcException into Throwable,
 *   or returns null conversion if not supported.
 *
 * Throwable may be any exception,
 * especially "business" exceptions declared by interface.
 *
 * XmlRpcException has two special fields which must be used:
 * - code (xml-rpc faultCode
 *   unique exception number,
 * - message (xml-rpc faultString)
 *   additional data useful to recreate exception.
 *
 *               EXAMPLE USAGE
 *
 * Server: please check pl.nn44.rchat.server.impl.Endpoints
 * Client: please check pl.nn44.rchat.client.impl.Clients
 *
 *              BONUS - ClientFactory fix
 *
 * Class: ClientFactoryFix
 *
 * Fixed org.apache.xmlrpc.client.util.ClientFactory class.
 * Changes:
 * - fix toString(), equals(), hashCode()
 *   These methods are not handled properly by original class.
 * </pre>
 */
package pl.nn44.xmlrpc;
