<#import "template.ftl" as layout>
<@layout.mainLayout active='account' bodyClass='user'; section>

    <div class="row">
        <div class="col-md-10">
            <h2>${msg("editAccountHtmlTitle")}</h2>
        </div>
        <div class="col-md-2 subtitle">
            <span class="subtitle"><span class="required">*</span> ${msg("requiredFields")}</span>
        </div>
    </div>

    <form action="${url.accountUrl}" class="form-horizontal" method="post">

        <input type="hidden" id="stateChecker" name="stateChecker" value="${stateChecker}">

        <#if !realm.registrationEmailAsUsername>
            <div class="form-group ${messagesPerField.printIfExists('username','has-error')}">
                <div class="col-sm-2 col-md-2">
                    <label for="username" class="control-label">${msg("username")}</label> <#if realm.editUsernameAllowed><span class="required">*</span></#if>
                </div>

                <div class="col-sm-10 col-md-10">
                    <input type="text" class="form-control" id="username" name="username" <#if !realm.editUsernameAllowed>disabled="disabled"</#if> value="${(account.username!'')}"/>
                </div>
            </div>
        </#if>

        <div class="form-group ${messagesPerField.printIfExists('email','has-error')}">
            <div class="col-sm-2 col-md-2">
            <label for="email" class="control-label">${msg("email")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="email" name="email" autofocus value="${(account.email!'')}"/>
            </div>
        </div>

        <div class="form-group ${messagesPerField.printIfExists('firstName','has-error')}">
            <div class="col-sm-2 col-md-2">
                <label for="firstName" class="control-label">${msg("firstName")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="firstName" name="firstName" value="${(account.firstName!'')}"/>
            </div>
        </div>

        <div class="form-group ${messagesPerField.printIfExists('lastName','has-error')}">
            <div class="col-sm-2 col-md-2">
                <label for="lastName" class="control-label">${msg("lastName")}</label> <span class="required">*</span>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="text" class="form-control" id="lastName" name="lastName" value="${(account.lastName!'')}"/>
            </div>
        </div>

        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <#if url.referrerURI??><a href="${url.referrerURI}">${kcSanitize(msg("backToApplication")?no_esc)}</a></#if>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Save">${msg("doSave")}</button>
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonDefaultClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Cancel">${msg("doCancel")}</button>
                </div>
            </div>
        </div>
    </form>

    <div class="row">
        <div class="col-md-10">
            <h2>${msg("changeAvatarHtmlTitle")}</h2>
        </div>
    </div>

    <#assign avatarUrl = url.accountUrl?replace("^(.*)(/account/?)(\\?(.*))?$", "$1/avatar-provider", 'r') />
    <form action="${avatarUrl}" id="avatarForm" class="form-horizontal" method="post" enctype="multipart/form-data">

        <img id="avatar-preview" src="${avatarUrl}" style="max-width:200px;" >
        
        <div class="form-group">
            <div class="col-sm-2 col-md-2">
                <label for="lastName" class="control-label">${msg("avatar")}</label>
            </div>

            <div class="col-sm-10 col-md-10">
                <input type="file" id="avatar" name="org-image">
            </div>
        </div>
        <div id="avatar-crop" style="display: none;"></div>

        <input type="hidden" name="stateChecker" value="${stateChecker}">

        <div class="form-group">
            <div id="kc-form-buttons" class="col-md-offset-2 col-md-10 submit">
                <div class="">
                    <button type="submit" class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonLargeClass!}" name="submitAction" value="Save">${msg("doSave")}</button>
                </div>
            </div>
        </div>
    </form>
    <script>
    (function() {
        var avatar = document.getElementById('avatar-crop');
        var avatarCrop = new Croppie(avatar, {viewport: {width: 100, height: 100, type: 'square'}, boundary: {width: 300, height: 300}});
        var form = document.getElementById("avatarForm");
        function readFile(input) {
            if (input.files && input.files[0]) {
                var reader = new FileReader();
                reader.onload = function (e) {
                    avatar.style.display = "block";
                    avatarCrop.bind({
                        url: e.target.result
                    })
                    
                }
                reader.readAsDataURL(input.files[0]);
            }
            else {
                swal("Sorry - you're browser doesn't support the FileReader API");
            }
        }
        document.getElementById("avatar").onchange = function(e){readFile(this);};
        form.onsubmit = async function(e){
            e.preventDefault();
            var data = new FormData(this);
            var img = await avatarCrop.result({type: 'blob', size: {width: 500, height: 500}});
            data.append("image", img, "avatar.png");
            let response = await fetch('${avatarUrl}', {
                method: 'POST',
                body: data
            });
            window.location.reload(false); 
            return false;
        };
    })();
    </script>

</@layout.mainLayout>