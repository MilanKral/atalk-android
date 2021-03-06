/*
 * Jitsi, the OpenSource Java VoIP and Instant Messaging client.
 *
 * Distributable under LGPL license. See terms of license at gnu.org.
 */
package org.xmpp.extensions.coin;

import org.xmpp.extensions.AbstractExtensionElement;

/**
 * URIs packet extension.
 *
 * @author Sebastien Vincent
 * @author Eng Chong Meng
 */
public class URIsExtension extends AbstractExtensionElement
{
    /**
     * The name of the element that contains the URIs data.
     */
    public static final String ELEMENT = "uris";

    /**
     * The namespace that URIs belongs to.
     */
    public static final String NAMESPACE = "";

    /**
     * Constructor.
     */
    public URIsExtension()
    {
        super(ELEMENT, NAMESPACE);
    }
}
