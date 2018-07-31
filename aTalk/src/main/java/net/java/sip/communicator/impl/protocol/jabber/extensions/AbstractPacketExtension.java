/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package net.java.sip.communicator.impl.protocol.jabber.extensions;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;

/**
 * A generic implementation of <tt>ExtensionElement</tt>. The purpose of this class is quite similar
 * to that of smack's {@link ExtensionElement} with the main difference being that this one is
 * meant primarily for extension rather than using as a fallback for unknown elements. We let for
 * example our descendants handle child elements and we automate attribute handling instead.
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public abstract class AbstractPacketExtension implements ExtensionElement
{
    /**
     * Clones the attributes, namespace and text of a specific <tt>AbstractPacketExtension</tt> into
     * a new <tt>AbstractPacketExtension</tt> instance of the same run-time type.
     *
     * @param src the <tt>AbstractPacketExtension</tt> to be cloned
     * @return a new <tt>AbstractPacketExtension</tt> instance of the run-time type of the specified
     * <tt>src</tt> which has the same attributes, namespace and text
     * @throws Exception if an error occurs during the cloning of the specified <tt>src</tt>
     */
    @SuppressWarnings("unchecked")
    public static <T extends AbstractPacketExtension> T clone(T src)
    {
        T dst = null;
        try {
            dst = (T) src.getClass().newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        // attributes
        for (String name : src.getAttributeNames()) {
            dst.setAttribute(name, src.getAttribute(name));
        }
        // namespace
        dst.setNamespace(src.getNamespace());
        // text
        dst.setText(src.getText());
        return dst;
    }

    /**
     * The name space of this packet extension. Should remain <tt>null</tt> if there's no namespace
     * associated with this element.
     */
    private String namespace;

    /**
     * The name space of this packet extension. Should remain <tt>null</tt> if there's no namespace
     * associated with this element.
     */
    private final String elementName;

    /**
     * A map of all attributes that this extension is currently using.
     */
    protected final Map<String, Object> attributes = new LinkedHashMap<>();

    /**
     * A list of all packets that are wrapped by this extension.
     */
    private final List<Stanza> packets = new LinkedList<>();

    /**
     * The text content of this packet extension, if any.
     */
    private String textContent;

    /**
     * A list of extensions registered with this element.
     */
    private final List<ExtensionElement> childExtensions = new ArrayList<>();

    /**
     * Creates an {@link AbstractPacketExtension} instance for the specified <tt>namespace</tt> and <tt>elementName</tt> .
     *
     * @param namespace the XML namespace for this element.
     * @param elementName the name of the element
     */
    protected AbstractPacketExtension(String elementName, String namespace)
    {
        this.elementName = elementName;
        this.namespace = namespace;
    }

    /**
     * Returns the name of the <tt>encryption</tt> element.
     *
     * @return the name of the <tt>encryption</tt> element.
     */
    public String getElementName()
    {
        return elementName;
    }

    /**
     * Set the XML namespace for this element.
     *
     * @param namespace the XML namespace for this element.
     */
    public void setNamespace(String namespace)
    {
        this.namespace = namespace;
    }

    /**
     * Returns the XML namespace for this element or <tt>null</tt> if the element does not live in a namespace of its own.
     *
     * @return the XML namespace for this element or <tt>null</tt> if the element does not live in a namespace of its own.
     */
    public String getNamespace()
    {
        return namespace;
    }

    /**
     * Returns an XML representation of this extension.
     *
     * @return an XML representation of this extension.
     */
    public XmlStringBuilder toXML()
    {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.prelude(getElementName(), getNamespace());

        // add the rest of the attributes if any
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            xml.optAttribute(entry.getKey(), entry.getValue().toString());
        }

        // add child elements if any
        List<? extends ExtensionElement> childElements = getChildExtensions();
        String text = getText();
        List<Stanza> packets = getPackets();

        if (childElements.size() == 0 && packets.size() == 0) {
            if ((text == null) || (text.length() == 0)) {
                xml.append("/>");
                return xml;
            }
            else
                xml.append('>');
        }
        else {
            synchronized (childElements) {
                if (childElements.isEmpty() && packets.isEmpty()
                        && ((text == null) || (text.length() == 0))) {
                    xml.append("/>");
                    return xml;
                }
                else {
                    xml.append(">");
                    for (ExtensionElement packExt : childElements)
                        xml.append(packExt.toXML());
                    for (Stanza packet : packets)
                        xml.append(packet.toXML());
                }
            }
        }

        // text content if any
        if ((text != null) && (text.trim().length() > 0))
            xml.append(StringUtils.escapeForXml(text));

        xml.closeElement(getElementName());
        return xml;
    }

    /**
     * Returns all sub-elements for this <tt>AbstractPacketExtension</tt> or <tt>null</tt> if there aren't any.
     *
     * Overriding extensions may need to override this method if they would like to have anything
     * more elaborate than just a list of extensions.
     *
     * @return the {@link List} of elements that this packet extension contains.
     */
    public List<? extends ExtensionElement> getChildExtensions()
    {
        return childExtensions;
    }

    /**
     * Adds the specified <tt>childExtension</tt> to the list of extensions registered with this packet.
     *
     * Overriding extensions may need to override this method if they would like to have anything
     * more elaborate than just a list of extensions (e.g. casting separate instances to more specific.
     *
     * @param childExtension the extension we'd like to add here.
     */
    public void addChildExtension(ExtensionElement childExtension)
    {
        childExtensions.add(childExtension);
    }

    /**
     * Returns the list of packets.
     *
     * @return the list of packets.
     */
    public List<Stanza> getPackets()
    {
        return packets;
    }

    /**
     * Adds packet to the list of packets.
     *
     * @param packet the packet to add.
     */
    public void addPacket(Stanza packet)
    {
        packets.add(packet);
    }

    /**
     * Sets the value of the attribute named <tt>name</tt> to <tt>value</tt>.
     *
     * @param name the name of the attribute that we are setting.
     * @param value an {@link Object} whose <tt>toString()</tt> method returns the XML value of the
     * attribute we are setting or <tt>null</tt> if we'd like to remove the attribute with the specified <tt>name</tt>.
     */
    public void setAttribute(String name, Object value)
    {
        synchronized (attributes) {
            if (value != null) {
                this.attributes.put(name, value);
            }
            else {
                this.attributes.remove(name);
            }
        }
    }

    /**
     * Removes the attribute with the specified <tt>name</tt> from the list of attributes registered
     * with this packet extension.
     *
     * @param name the name of the attribute that we are removing.
     */
    public void removeAttribute(String name)
    {
        synchronized (attributes) {
            attributes.remove(name);
        }
    }

    /**
     * Returns the attribute with the specified <tt>name</tt> from the list of attributes registered
     * with this packet extension.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     * @return the value of the specified <tt>attribute</tt> or <tt>null</tt> if no such attribute
     * is currently registered with this extension.
     */
    public Object getAttribute(String attribute)
    {
        synchronized (attributes) {
            return attributes.get(attribute);
        }
    }

    /**
     * Returns the string value of the attribute with the specified <tt>name</tt>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     * @return the String value of the specified <tt>attribute</tt> or <tt>null</tt> if no such
     * attribute is currently registered with this extension.
     */
    public String getAttributeAsString(String attribute)
    {
        synchronized (attributes) {
            Object attributeVal = attributes.get(attribute);
            return attributeVal == null ? null : attributeVal.toString();
        }
    }

    /**
     * Returns the <tt>int</tt> value of the attribute with the specified <tt>name</tt>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     * @return the <tt>int</tt> value of the specified <tt>attribute</tt> or <tt>-1</tt> if no such
     * attribute is currently registered with this extension.
     */
    public int getAttributeAsInt(String attribute)
    {
        return getAttributeAsInt(attribute, -1);
    }

    /**
     * Returns the <tt>int</tt> value of the attribute with the specified <tt>name</tt>.
     *
     * @param attribute the name of the attribute that we'd like to retrieve
     * @param defaultValue the <tt>int</tt> to be returned as the value of the specified attribute if no such
     * attribute is currently registered with this extension
     * @return the <tt>int</tt> value of the specified <tt>attribute</tt> or <tt>defaultValue</tt>
     * if no such attribute is currently registered with this extension
     */
    public int getAttributeAsInt(String attribute, int defaultValue)
    {
        synchronized (attributes) {
            String value = getAttributeAsString(attribute);
            return (value == null) ? defaultValue : Integer.parseInt(value);
        }
    }

    /**
     * Tries to parse the value of the specified <tt>attribute</tt> as an <tt>URI</tt> and returns
     * it.
     *
     * @param attribute the name of the attribute that we'd like to retrieve.
     * @return the <tt>URI</tt> value of the specified <tt>attribute</tt> or <tt>null</tt> if no
     * such attribute is currently registered with this extension.
     * @throws IllegalArgumentException if <tt>attribute</tt> is not a valid {@link URI}
     */
    public URI getAttributeAsURI(String attribute)
            throws IllegalArgumentException
    {
        synchronized (attributes) {
            String attributeVal = getAttributeAsString(attribute);

            if (attributeVal == null)
                return null;

            try {
                return new URI(attributeVal);
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }
    }

    /**
     * Gets the names of the attributes which currently have associated values in this extension.
     *
     * @return the names of the attributes which currently have associated values in this extension
     */
    public List<String> getAttributeNames()
    {
        synchronized (attributes) {
            return new ArrayList<>(attributes.keySet());
        }
    }

    /**
     * Specifies the text content of this extension.
     *
     * @param text the text content of this extension.
     */
    public void setText(String text)
    {
        this.textContent = text;
    }

    /**
     * Returns the text content of this extension or <tt>null</tt> if no text content has been specified so far.
     *
     * @return the text content of this extension or <tt>null</tt> if no text content has been specified so far.
     */
    public String getText()
    {
        return textContent;
    }

    /**
     * Returns this packet's first direct child extension that matches the specified <tt>type</tt>.
     *
     * @param <T> the specific type of <tt>ExtensionElement</tt> to be returned
     * @param type the <tt>Class</tt> of the extension we are looking for.
     * @return this packet's first direct child extension that matches specified <tt>type</tt> or
     * <tt>null</tt> if no such child extension was found.
     */
    public <T extends ExtensionElement> T getFirstChildOfType(Class<T> type)
    {
        List<? extends ExtensionElement> childExtensions = getChildExtensions();
        synchronized (childExtensions) {
            for (ExtensionElement extension : childExtensions) {
                if (type.isInstance(extension)) {
                    @SuppressWarnings("unchecked")
                    T extensionAsType = (T) extension;
                    return extensionAsType;
                }
            }
        }
        return null;
    }

    /**
     * Returns this packet's direct child extensions that match the specified <tt>type</tt>.
     *
     * @param <T> the specific <tt>ExtensionElement</tt> type of child extensions to be returned
     * @param type the <tt>Class</tt> of the extension we are looking for.
     * @return a (possibly empty) list containing all of this packet's direct child extensions that
     * match the specified <tt>type</tt>
     */
    public <T extends ExtensionElement> List<T> getChildExtensionsOfType(Class<T> type)
    {
        List<? extends ExtensionElement> childExtensions = getChildExtensions();
        List<T> result = new ArrayList<>();

        if (childExtensions == null)
            return result;

        synchronized (childExtensions) {
            for (ExtensionElement extension : childExtensions) {
                if (type.isInstance(extension)) {
                    @SuppressWarnings("unchecked")
                    T extensionAsType = (T) extension;
                    result.add(extensionAsType);
                }
            }
        }
        return result;
    }
}
