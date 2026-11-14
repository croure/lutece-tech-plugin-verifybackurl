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
    private static final String PORT_CHARACTERS = "[0-9]+";
    private static final String PORT_SEPARATOR = ":";
    private static final String PATH_SEPARATOR = "/";
    
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
     * Test if the back url matches an authorized pattern {@code scheme://host[:port][path]}, each part of the url, read by {@link URI},
     * against the same part of the pattern:
     * <ul>
     * <li>the scheme and the host are compared without case; in the host of a pattern, a {@code *} stands for one or more characters
     * of a host name, and a {@code **} at its start (any text before the host) is ignored: the host must match exactly;</li>
     * <li>a {@code **} at the end of the host of a pattern accepts any port and any path: {@code http*://**localhost**} matches the
     * host localhost only, with or without port and path;</li>
     * <li>otherwise the port must match the port of the pattern (no port when the pattern has none), and the path, with the query and
     * the fragment, must match the path of the pattern (Ant matching, case-sensitive; nothing when the pattern has no path):
     * {@code http*://*.paris.mdp/**} matches the subdomains of paris.mdp only, with any path.</li>
     * </ul>
     * 
     * @param strPattern
     *            the authorized pattern
     * @param strBackUrl
     *            the back url to test
     * @return true if the back url matches the pattern
     */
    public static boolean matchAuthorizedPattern( String strPattern, String strBackUrl )
    {
        int nHostStart = strPattern.indexOf( SCHEME_SEPARATOR );
        if ( nHostStart < 0 )
        {
            // A pattern without host, such as a relative path
            return PATH_MATCHER.match( strPattern, strBackUrl );
        }

        URI uri;
        try
        {
            uri = new URI( strBackUrl );
        }
        catch( URISyntaxException e )
        {
            return false;
        }
        if ( uri.getScheme( ) == null || StringUtils.isEmpty( uri.getHost( ) )
                || !PATH_MATCHER.match( strPattern.substring( 0, nHostStart ).toLowerCase( Locale.ROOT ), uri.getScheme( ).toLowerCase( Locale.ROOT ) ) )
        {
            return false;
        }

        String strAuthorityPattern = strPattern.substring( nHostStart + SCHEME_SEPARATOR.length( ) );
        String strPathPattern = StringUtils.EMPTY;
        Matcher matcherEnd = HOST_PATTERN_END.matcher( strAuthorityPattern );
        if ( matcherEnd.find( ) )
        {
            strPathPattern = strAuthorityPattern.substring( matcherEnd.start( ) );
            strAuthorityPattern = strAuthorityPattern.substring( 0, matcherEnd.start( ) );
        }
        boolean bAnyPortAndPath = strAuthorityPattern.endsWith( ANY_TEXT );
        strAuthorityPattern = StringUtils.removeEnd( strAuthorityPattern, ANY_TEXT );
        if ( HOST_PATTERN_ANY_START.matcher( strAuthorityPattern ).find( ) )
        {
            // **localhost: the text before the host in the url, not a part of the host
            strAuthorityPattern = strAuthorityPattern.substring( ANY_TEXT.length( ) );
        }
        String strHostPattern = StringUtils.substringBefore( strAuthorityPattern, PORT_SEPARATOR );

        // http*://** sets no host
        if ( !strHostPattern.isEmpty( ) && !uri.getHost( ).toLowerCase( Locale.ROOT )
                .matches( toWildcardRegex( strHostPattern.toLowerCase( Locale.ROOT ), HOST_NAME_CHARACTERS ) ) )
        {
            return false;
        }
        if ( bAnyPortAndPath )
        {
            return true;
        }

        String strPort = ( uri.getPort( ) < 0 ) ? StringUtils.EMPTY : String.valueOf( uri.getPort( ) );
        String strPortPattern = strAuthorityPattern.contains( PORT_SEPARATOR ) ? StringUtils.substringAfter( strAuthorityPattern, PORT_SEPARATOR )
                : StringUtils.EMPTY;
        if ( !strPort.matches( toWildcardRegex( strPortPattern, PORT_CHARACTERS ) ) )
        {
            return false;
        }

        String strPath = StringUtils.defaultString( uri.getRawPath( ) ) + ( ( uri.getRawQuery( ) == null ) ? StringUtils.EMPTY : "?" + uri.getRawQuery( ) )
                + ( ( uri.getRawFragment( ) == null ) ? StringUtils.EMPTY : "#" + uri.getRawFragment( ) );
        if ( strPathPattern.isEmpty( ) )
        {
            return strPath.isEmpty( );
        }
        return PATH_MATCHER.match( strPathPattern, strPath.isEmpty( ) ? PATH_SEPARATOR : strPath );
    }

    /**
     * Test if the host of the back url has no dot (such as localhost) and is named by an authorized pattern
     * 
     * @param strBackUrl
     *            the back url to test
     * @return true if the host of the back url has no dot and an authorized pattern names it
     */
    public static boolean isHostWithoutDotNamedByAuthorizedPattern( String strBackUrl )
    {
        String strAuthorizedDomains = AppPropertiesService.getProperty( VerifyBackUrlConstants.PROPERTY_AUTHORIZED_DOMAINS_BACK_URL );

        return !StringUtils.isEmpty( strAuthorizedDomains )
                && isHostWithoutDotNamedByPattern( strAuthorizedDomains.split( VerifyBackUrlConstants.COMMA ), strBackUrl );
    }

    /**
     * Test if the host of the back url has no dot (such as localhost) and is named by one of the patterns: the host of the pattern is
     * this host written without {@code *}, and the back url matches the pattern. A pattern that accepts any host does not name it.
     * 
     * @param patterns
     *            the authorized patterns
     * @param strBackUrl
     *            the back url to test
     * @return true if the host of the back url has no dot and a pattern names it
     */
    static boolean isHostWithoutDotNamedByPattern( String [ ] patterns, String strBackUrl )
    {
        String strHost;
        try
        {
            strHost = new URI( strBackUrl ).getHost( );
        }
        catch( URISyntaxException e )
        {
            return false;
        }
        if ( StringUtils.isEmpty( strHost ) || strHost.contains( "." ) )
        {
            return false;
        }

        for ( String strPattern : patterns )
        {
            int nHostStart = strPattern.indexOf( SCHEME_SEPARATOR );
            if ( nHostStart < 0 )
            {
                continue;
            }
            String strHostPattern = strPattern.substring( nHostStart + SCHEME_SEPARATOR.length( ) );
            Matcher matcherEnd = HOST_PATTERN_END.matcher( strHostPattern );
            if ( matcherEnd.find( ) )
            {
                strHostPattern = strHostPattern.substring( 0, matcherEnd.start( ) );
            }
            strHostPattern = StringUtils.removeEnd( strHostPattern, ANY_TEXT );
            if ( HOST_PATTERN_ANY_START.matcher( strHostPattern ).find( ) )
            {
                strHostPattern = strHostPattern.substring( ANY_TEXT.length( ) );
            }
            strHostPattern = StringUtils.substringBefore( strHostPattern, PORT_SEPARATOR );
            if ( strHostPattern.equalsIgnoreCase( strHost ) && matchAuthorizedPattern( strPattern, strBackUrl ) )
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Build the regular expression of a pattern where a {@code *} stands for one or more given characters
     * 
     * @param strPattern
     *            the pattern
     * @param strCharacters
     *            the regular expression of the characters a {@code *} stands for
     * @return the regular expression
     */
    private static String toWildcardRegex( String strPattern, String strCharacters )
    {
        return Arrays.stream( strPattern.split( "\\*+", -1 ) ).map( Pattern::quote ).collect( Collectors.joining( strCharacters ) );
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
