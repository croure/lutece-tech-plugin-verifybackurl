/*
 * Copyright (c) 2002-2026, City of Paris
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
package fr.paris.lutece.plugins.verifybackurl.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import fr.paris.lutece.test.LuteceTestCase;
import jakarta.inject.Inject;

/**
 * Checks that a host without a dot is accepted only when an authorized pattern of verifybackurl.properties names it
 * (http*://**localhost** by default).
 */
public class CharactersDomainConstraintTest extends LuteceTestCase
{
    @Inject
    private CharactersDomainConstraint _constraint;

    @Test
    public void testHostWithDot( )
    {
        assertTrue( _constraint.checkConstraint( "http://site.paris.mdp" ) );
        assertTrue( _constraint.checkConstraint( "https://site.paris.mdp:8443/lutece/jsp/site/Portal.jsp" ) );
    }

    @Test
    public void testHostWithoutDotNamedByAPattern( )
    {
        assertTrue( _constraint.checkConstraint( "http://localhost" ) );
        assertTrue( _constraint.checkConstraint( "http://localhost:8080/app/page" ) );
        assertTrue( _constraint.checkConstraint( "https://LOCALHOST:8443" ) );
    }

    @Test
    public void testHostWithoutDotNotNamed( )
    {
        assertFalse( _constraint.checkConstraint( "http://intranet" ) );
        assertFalse( _constraint.checkConstraint( "http://intranet:8080/app/page" ) );
        assertFalse( _constraint.checkConstraint( "http://mylocalhost/app" ) );
        assertFalse( _constraint.checkConstraint( "localhost" ) );
    }
}
