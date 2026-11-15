${ pageContext.setAttribute( 'strContent', authorizedUrlJspBean.processController ( pageContext.request , pageContext.response ) ) }

<%@ page errorPage="../../ErrorPage.jsp" %>
<%
    // null: the controller has already written the response (the JSON list of a @ResponseBody view, a redirection)
    if ( pageContext.getAttribute( "strContent" ) != null )
    {
%>
<jsp:include page="../../AdminHeader.jsp" />

${ pageContext.getAttribute( 'strContent' ) }

<%@ include file="../../AdminFooter.jsp" %>
<%
    }
%>
