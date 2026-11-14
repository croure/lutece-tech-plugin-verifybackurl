/*
 * Copyright (c) 2002-2016, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.verifybackurl.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Checks that the host of the back url matches exactly the host of the authorized patterns (the default patterns of
 * verifybackurl.properties).
 */
public class VerifiyBackUrlUtilsTest
{
    private static final String PATTERN_LOCALHOST = "http*://**localhost**";
    private static final String PATTERN_DOMAIN = "http*://*.paris.mdp";
    private static final String PATTERN_DOMAIN_PATH = "http*://*.paris.mdp/**";

    @Test
    public void testAllowedUrls( )
    {
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "http://localhost" ) );
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "http://localhost:8080" ) );
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "https://localhost:8443" ) );
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN, "http://site.paris.mdp" ) );
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "https://site.paris.mdp/lutece/jsp/site/Portal.jsp?page=x" ) );
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "http://www.site.paris.mdp/a/b" ) );
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( "http*://**", "https://any.example.org" ) );
        assertTrue( VerifiyBackUrlUtils.matchAuthorizedPattern( "/lutece/**", "/lutece/jsp/site/Portal.jsp" ) );
    }

    @Test
    public void testRefusedUrls( )
    {
        // Host that only starts or ends with the authorized host
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "http://localhost.example.org" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "http://localhost.example.org:8080" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "http://mylocalhost" ) );
        // Authorized host written after the host of the url
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "http://example.org?localhost" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_LOCALHOST, "http://example.org#localhost" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN, "http://example.org?site.paris.mdp" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "http://example.org#.paris.mdp" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "http://site.paris.mdp.example.org/a" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "http://example.org/site.paris.mdp/a" ) );
        // Domain itself, other scheme, not a url
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "http://paris.mdp/a" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "ftp://site.paris.mdp/a" ) );
        assertFalse( VerifiyBackUrlUtils.matchAuthorizedPattern( PATTERN_DOMAIN_PATH, "http://site.paris.mdp/a b" ) );
    }
}
