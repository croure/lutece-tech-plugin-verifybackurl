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
package fr.paris.lutece.plugins.verifybackurl.web;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * The controller JSP of the authorized URLs. When processController returns null, the controller has already written the response (the JSON
 * list loaded by the paginationAjax table, a redirection): the JSP must then write neither the admin header nor the admin footer, otherwise
 * the HTML of the page is appended after the JSON and the table shows no row.
 */
public class ManageAuthorizedUrlsJspTest
{
    private static final String JSP = "webapp/jsp/admin/plugins/verifybackurl/ManageAuthorizedUrls.jsp";
    private static final Pattern GUARD = Pattern.compile( "if\\s*\\(\\s*pageContext\\.getAttribute\\(\\s*\"strContent\"\\s*\\)\\s*!=\\s*null\\s*\\)\\s*\\{" );

    @Test
    public void testHeaderAndFooterOnlyWhenTheControllerReturnsAPage( ) throws IOException
    {
        String strJsp = Files.readString( Path.of( JSP ), StandardCharsets.UTF_8 );

        int nController = strJsp.indexOf( "processController" );
        int nHeader = strJsp.indexOf( "AdminHeader.jsp" );
        int nFooter = strJsp.indexOf( "AdminFooter.jsp" );
        assertTrue( nController >= 0 && nHeader >= 0 && nFooter >= 0, strJsp );

        Matcher guard = GUARD.matcher( strJsp );
        assertTrue( guard.find( nController ), "The header and the footer must be written only when processController returned a page:\n" + strJsp );
        int nClose = strJsp.indexOf( '}', nFooter );
        assertTrue( guard.end( ) < nHeader && nHeader < nFooter && nClose > nFooter,
                "The admin header and footer must be inside the block guarded by the null check of the controller result:\n" + strJsp );
    }
}
