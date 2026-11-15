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
package fr.paris.lutece.plugins.verifybackurl.web;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import fr.paris.lutece.plugins.verifybackurl.business.AuthorizedUrl;
import fr.paris.lutece.plugins.verifybackurl.business.AuthorizedUrlHome;
import fr.paris.lutece.portal.business.user.AdminUser;
import fr.paris.lutece.portal.service.admin.AccessDeniedException;
import fr.paris.lutece.portal.service.message.AdminMessage;
import fr.paris.lutece.portal.service.message.AdminMessageService;
import fr.paris.lutece.portal.service.security.SecurityTokenHandler;
import fr.paris.lutece.test.AdminUserUtils;
import fr.paris.lutece.test.LuteceTestCase;
import fr.paris.lutece.test.mocks.MockHttpServletRequest;
import fr.paris.lutece.test.mocks.MockHttpServletResponse;
import jakarta.inject.Inject;

/**
 * Checks that the requests made by the management page pass the admin security token filter: the list loaded with GET is a view, and the
 * removal posted from the confirmation message carries the token of the removal action.
 */
public class AuthorizedUrlJspBeanTest extends LuteceTestCase
{
    private static final String SERVLET_PATH = "/jsp/admin/plugins/verifybackurl/ManageAuthorizedUrls.jsp";
    private static final String RIGHT = "VERIFYBACKURL_MANAGEMENT";
    private static final String ACTION_REMOVE = "removeAuthorizedUrl";

    @Inject
    private AuthorizedUrlJspBean _instance;
    @Inject
    private SecurityTokenHandler _securityTokenHandler;

    private static AuthorizedUrl createAuthorizedUrl( String strName )
    {
        AuthorizedUrl authorizedUrl = new AuthorizedUrl( );
        authorizedUrl.setName( strName );
        authorizedUrl.setUrl( "https://" + strName + ".example.org/**" );
        authorizedUrl.setApplicationCode( "test" );
        AuthorizedUrlHome.create( authorizedUrl );
        return authorizedUrl;
    }

    private static MockHttpServletRequest newRequest( String strMethod )
    {
        MockHttpServletRequest request = new MockHttpServletRequest( );
        request.setMethod( strMethod );
        request.setServletPath( SERVLET_PATH );
        AdminUserUtils.registerAdminUserWithRight( request, new AdminUser( ), RIGHT );
        return request;
    }

    @Test
    public void testAuthorizedUrlItemsIsAView( ) throws Exception
    {
        AuthorizedUrl authorizedUrl = createAuthorizedUrl( "listitem" );
        try
        {
            MockHttpServletRequest request = newRequest( "GET" );
            request.addParameter( "view", "manageAuthorizedUrls" );
            _instance.processController( request, new MockHttpServletResponse( ) );

            request.setParameter( "view", "getAuthorizedUrlItems" );
            request.addParameter( "page", "1" );
            assertTrue( _securityTokenHandler.shouldNotFilter( request ), "The list loaded with GET must not need a security token" );

            MockHttpServletResponse response = new MockHttpServletResponse( );
            assertNull( _instance.processController( request, response ), "The JSON view writes the response itself: the JSP must add nothing" );

            // The whole body must be one JSON document (the paginationAjax table parses it with JSON.parse)
            String strBody = response.getContentAsString( );
            JsonNode items = new ObjectMapper( ).readTree( strBody );
            assertTrue( items.isArray( ), strBody );
            boolean bFound = false;
            for ( JsonNode item : items )
            {
                bFound |= authorizedUrl.getUrl( ).equals( item.path( "url" ).asText( ) );
            }
            assertTrue( bFound, strBody );
        }
        finally
        {
            AuthorizedUrlHome.remove( authorizedUrl.getId( ) );
        }
    }

    @Test
    public void testRemoveWithTheTokenOfTheConfirmation( ) throws AccessDeniedException
    {
        AuthorizedUrl authorizedUrl = createAuthorizedUrl( "removal" );
        try
        {
            MockHttpServletRequest request = newRequest( "GET" );
            request.addParameter( "view", "confirmRemoveAuthorizedUrl" );
            request.addParameter( "id", String.valueOf( authorizedUrl.getId( ) ) );
            assertTrue( _securityTokenHandler.shouldNotFilter( request ), "The confirmation opened by a link must not need a security token" );
            _instance.processController( request, new MockHttpServletResponse( ) );

            AdminMessage message = AdminMessageService.getMessage( request );
            assertNotNull( message );
            assertTrue( message.getUrl( ).contains( "action=" + ACTION_REMOVE ), message.getUrl( ) );
            Object token = message.getRequestParameters( ).get( SecurityTokenHandler.MARK_CSRF_TOKEN );
            assertNotNull( token, "The confirmation form must post the token of the removal" );

            // The confirmation form, posted in the same session
            request.setMethod( "POST" );
            request.setParameter( "view", new String [ 0] );
            request.setParameter( "action", ACTION_REMOVE );
            request.setParameter( SecurityTokenHandler.MARK_CSRF_TOKEN, (String) token );
            assertFalse( _securityTokenHandler.shouldNotFilter( request ), "The removal must be checked by the security token filter" );
            assertTrue( _securityTokenHandler.validate( request, _securityTokenHandler.resolveTokenAction( request, ACTION_REMOVE ) ) );

            _instance.processController( request, new MockHttpServletResponse( ) );
            assertNull( AuthorizedUrlHome.findByPrimaryKey( authorizedUrl.getId( ) ) );
        }
        finally
        {
            AuthorizedUrlHome.remove( authorizedUrl.getId( ) );
        }
    }
}
