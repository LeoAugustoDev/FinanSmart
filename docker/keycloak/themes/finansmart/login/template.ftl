<#macro registrationLayout bodyClass="" displayInfo=false displayMessage=true displayRequiredFields=false activeTab="">
<!DOCTYPE html>
<html lang="${lang}">

<head>
    <meta charset="utf-8">
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
    <meta name="viewport" content="width=device-width, initial-scale=1" />
    <title>${msg("loginTitle",(realm.displayName!''))}</title>
    <link rel="icon" type="image/svg+xml" href="${url.resourcesPath}/img/logo.svg" />
    <#if properties.styles?has_content>
        <#list properties.styles?split(' ') as style>
            <link href="${url.resourcesPath}/${style}" rel="stylesheet" />
        </#list>
    </#if>
    <script type="importmap">
        {
            "imports": {
                "rfc4648": "${url.resourcesCommonPath}/vendor/rfc4648/rfc4648.js"
            }
        }
    </script>
    <#if scripts??>
        <#list scripts as script>
            <script src="${script}" type="text/javascript"></script>
        </#list>
    </#if>
    <script type="module">
        import { startSessionPolling } from "${url.resourcesPath}/js/authChecker.js";

        startSessionPolling(
            "${url.ssoLoginInOtherTabsUrl?no_esc}"
        );
    </script>
    <script type="module">
        document.addEventListener("click", (event) => {
            const link = event.target.closest("a[data-once-link]");

            if (!link) {
                return;
            }

            if (link.getAttribute("aria-disabled") === "true") {
                event.preventDefault();
                return;
            }

            link.setAttribute("role", "link");
            link.setAttribute("aria-disabled", "true");
        });
    </script>
    <#if authenticationSession??>
        <script type="module">
            import { checkAuthSession } from "${url.resourcesPath}/js/authChecker.js";

            checkAuthSession(
                "${authenticationSession.authSessionIdHash}"
            );
        </script>
    </#if>
</head>

<body class="fs-body" data-page-id="login-${pageId}">
<main class="fs-page">
    <img class="fs-logo" src="${url.resourcesPath}/img/logo.svg" alt="${msg('fsLogoAlt')}" width="46" height="46" />

    <#if activeTab?has_content>
        <nav class="fs-tabs" aria-label="${msg('fsLogoAlt')}">
            <a class="fs-tab<#if activeTab = 'login'> fs-tab-active</#if>" href="${url.loginUrl}"<#if activeTab = 'login'> aria-current="page"</#if>>${msg("fsTabLogin")}</a>
            <#if realm.registrationAllowed && !registrationDisabled??>
                <a class="fs-tab<#if activeTab = 'register'> fs-tab-active</#if>" href="${url.registrationUrl}"<#if activeTab = 'register'> aria-current="page"</#if>>${msg("fsTabRegister")}</a>
            </#if>
        </nav>
    </#if>

    <header class="fs-header">
        <#if !(auth?has_content && auth.showUsername() && !auth.showResetCredentials())>
            <h1 id="kc-page-title"><#nested "header"></h1>
        <#else>
            <#nested "show-username">
            <div id="kc-username" class="fs-attempted-user">
                <span id="kc-attempted-username">${auth.attemptedUsername}</span>
                <a id="reset-login" href="${url.loginRestartFlowUrl}" aria-label="${msg('restartLoginTooltip')}">${msg("restartLoginTooltip")}</a>
            </div>
        </#if>
        <#if activeTab?has_content>
            <p class="fs-subtitle"><#nested "subtitle"></p>
        </#if>
        <#if displayRequiredFields>
            <p class="fs-required-hint">* ${msg("requiredFields")}</p>
        </#if>
    </header>

    <#-- Ações iniciadas pela aplicação não devem ver avisos sobre a própria ação durante o login. -->
    <#if displayMessage && message?has_content && (message.type != 'warning' || !isAppInitiatedAction??)>
        <div class="${properties.kcAlertClass!} fs-alert-${message.type}" role="alert">
            <span class="${properties.kcAlertTitleClass!}">${kcSanitize(message.summary)?no_esc}</span>
        </div>
    </#if>

    <#nested "form">

    <#if auth?has_content && auth.showTryAnotherWayLink()>
        <form id="kc-select-try-another-way-form" action="${url.loginAction}" method="post">
            <div class="${properties.kcFormGroupClass!}">
                <input type="hidden" name="tryAnotherWay" value="on"/>
                <a href="#" id="try-another-way"
                   onclick="document.forms['kc-select-try-another-way-form'].requestSubmit();return false;">${msg("doTryAnotherWay")}</a>
            </div>
        </form>
    </#if>

    <#nested "socialProviders">

    <#if displayInfo>
        <div id="kc-info" class="${properties.kcSignUpClass!}">
            <#nested "info">
        </div>
    </#if>

    <footer class="fs-footer">${msg("fsFooter")}</footer>
</main>
</body>
</html>
</#macro>
