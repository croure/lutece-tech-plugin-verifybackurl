/*
 * Copyright (c) 2002-2014, Mairie de Paris
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

import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.http.AntPathMatcher;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;



public class VerifiyBackUrlUtils
{
    
    public final static AntPathMatcher PATH_MATCHER = new AntPathMatcher( );
    private static final String SCHEME_SEPARATOR = "://";
    private static final String ANY_TEXT = "**";
    private static final Pattern HOST_PATTERN_END = Pattern.compile( "[/?#]" );
    private static final Pattern HOST_PATTERN_ANY_START = Pattern.compile( "^\\*\\*[a-zA-Z0-9]" );
    private static final String HOST_NAME_CHARACTERS = "[a-z0-9.-]+";
    
    /**
     * Test if the BacUrl is a valid url
     * 
     * @param strBackUrl
     *            the back url to test
     * @return Test if the BackUrl is a valid url
     */
    public static boolean isValidBackUrl( String strBackUrl )
    {
        boolean isValid = true;
        String strAuthorizedDomains = AppPropertiesService.getProperty( VerifyBackUrlConstants.PROPERTY_AUTHORIZED_DOMAINS_BACK_URL );
   
        // test on domains url
        if ( !StringUtils.isEmpty( strAuthorizedDomains ) )
        {
            isValid = false;

            String [ ] tabAuthorizedDomains = strAuthorizedDomains.split( VerifyBackUrlConstants.COMMA );

            for ( int i = 0; i < tabAuthorizedDomains.length; i++ )
            {
                if ( matchAuthorizedPattern( tabAuthorizedDomains [i], strBackUrl ) )
                {
                    isValid = true;

                    break;
                }
            }
        }

        return isValid;
    }
    
    /**
     * Test if the back url matches an authorized pattern: the whole url matches the pattern, and the host of the url, read by
     * {@link URI}, matches exactly the host of the pattern. In the host of a pattern, a {@code *} stands for one or more characters
     * of a host name, and the {@code **} at its start or end (any text before or after the host in the url) as well as the port are
     * ignored: {@code http*://**localhost**} matches the host localhost only, {@code http*://*.paris.mdp/**} the subdomains of
     * paris.mdp only.
     * 
     * @param strPattern
     *            the authorized pattern
     * @param strBackUrl
     *            the back url to test
     * @return true if the back url matches the pattern
     */
    public static boolean matchAuthorizedPattern( String strPattern, String strBackUrl )
    {
        if ( !PATH_MATCHER.match( strPattern, strBackUrl ) )
        {
            return false;
        }
        int nHostStart = strPattern.indexOf( SCHEME_SEPARATOR );
        if ( nHostStart < 0 )
        {
            // A pattern without host, such as a relative path
            return true;
        }
        String strHostPattern = strPattern.substring( nHostStart + SCHEME_SEPARATOR.length( ) );
        Matcher matcherEnd = HOST_PATTERN_END.matcher( strHostPattern );
        if ( matcherEnd.find( ) )
        {
            strHostPattern = strHostPattern.substring( 0, matcherEnd.start( ) );
        }
        strHostPattern = StringUtils.substringBefore( StringUtils.removeEnd( strHostPattern, ANY_TEXT ), ":" );
        if ( HOST_PATTERN_ANY_START.matcher( strHostPattern ).find( ) )
        {
            // **localhost: the text before the host in the url, not a part of the host
            strHostPattern = strHostPattern.substring( ANY_TEXT.length( ) );
        }

        String strHost;
        try
        {
            strHost = new URI( strBackUrl ).getHost( );
        }
        catch( URISyntaxException e )
        {
            return false;
        }
        if ( StringUtils.isEmpty( strHost ) )
        {
            return false;
        }
        if ( strHostPattern.isEmpty( ) )
        {
            // http*://** sets no host
            return true;
        }

        String strRegex = Arrays.stream( strHostPattern.toLowerCase( Locale.ROOT ).split( "\\*+", -1 ) ).map( Pattern::quote )
                .collect( Collectors.joining( HOST_NAME_CHARACTERS ) );
        return strHost.toLowerCase( Locale.ROOT ).matches( strRegex );
    }

    /**
     * Test if the BacUrl is a valid url
     * 
     * @param strBackUrl
     *            the back url to test
     * @return Test if the BackUrl is a valid url
     */
    public static boolean containsNoUnauthorizedHTML( String strBackUrl )
    {
        boolean isValid = true;
        String strPatterUnAuthorizedHTML = AppPropertiesService.getProperty( VerifyBackUrlConstants.PROPERTY_AUTHORIZED_HTML );
        isValid = !strBackUrl.matches( strPatterUnAuthorizedHTML );

        return isValid;
    }

    /**
     * Test if the BackUrl is a valid url
     * 
     * @param strBackUrl
     *            the back url to test
     * @return Test if the BackUrl is a valid url
     */
    public static boolean containsUnauthorizedCharactersDomain( String strBackUrl )
    { 
        String strPatterUnAuthorizedCharactersDomain = AppPropertiesService.getProperty( VerifyBackUrlConstants.PROPERTY_UNAUTHORIZED_CHARACTERS_DOMAIN );

        return strBackUrl.matches( strPatterUnAuthorizedCharactersDomain );
    }
    
    
    /**
     * Compare to base url
     * @param url1 
     * @param url2
     * @return a boolean if base url of the two input Strings are the same an no empty.
     */
    public static boolean compareBaseUrl( String url1, String url2 )
    {
        String strBaseUrl1 = getBaseUrl( url1 );
        String strBaseUrl2 = getBaseUrl( url2 );
        if ( !StringUtils.isEmpty( strBaseUrl1 ) && strBaseUrl1.equals( strBaseUrl2 ) )
        {
            return true;
        }
        return false;
    }
    
    /**
     * Get base url for input Url : http://site.paris.mdp/webapp/jsp ==> site.paris.mdp/webapp
     * @param strUrl
     * @return the base Url
     */
    public static String getBaseUrl ( String strUrl )
    {
        String strRegexpBaseUrl = AppPropertiesService.getProperty( VerifyBackUrlConstants.PROPERTY_REGEXP_BASE_URL );
        Pattern regex = Pattern.compile( strRegexpBaseUrl );
        Matcher regexMatcher = regex.matcher( strUrl );
        if (regexMatcher.find( ) ) {
            return regexMatcher.group( 2 );
        }
        return "";
    }
    
    /**
     * Store service in session
     * @param request the HttpServletRequest
     * @param strBackUrl the backUrl to store in session
     * @param strSessionAttributeName the strSessionAttributeName who must store the backurl
     * 
     */
    public static void storeBackUrlInSession ( HttpServletRequest request, String strBackUrl,String strSessionAttributeName )
    {
        HttpSession session = request.getSession( true ); 
        session.setAttribute( strSessionAttributeName , strBackUrl );
    }
    
    /**
     * Drop Service in session
     * @param request the HttpServletrequest 
     * @param strSessionAttributeName the strSessionAttributeName who must store the backurl
     */
    
    public static void dropBackUrlInSession ( HttpServletRequest request,String strSessionAttributeName )
    {
        HttpSession session = request.getSession( true );
        session.removeAttribute( strSessionAttributeName );
        
    }
    
    
    /**
     * Return the back url in session
     * @param strSessionAttributeName the strSessionAttributeName who must store the backurl
     * @param request the HttpServletRequest
     */
    public static String getBackUrlInSession ( HttpServletRequest request,String strSessionAttributeName)
    {
        HttpSession session = request.getSession( true ); 
        return (String) session.getAttribute( strSessionAttributeName);
    }
    
    /**
     * encode url in base64
     * @param strUrl the url to encode
     * @return an url encode in base64
     */
    public  static String  encodeUrl(String strUrl)
    {
    	return !StringUtils.isEmpty(strUrl) ?  new String(Base64.getUrlEncoder().encode(strUrl.getBytes( StandardCharsets.UTF_8 ))):"";
    	
    }
    
    
    
}
