$(document).ready(function () {
    $("#languageDropdownMenuButton a").click(function (e) {
        e.preventDefault();

        const selectedLanguage = $(this).attr("value");
        const url = new URL(window.location.href);

        url.searchParams.set("lang", selectedLanguage);
        window.location.href = url.toString();

        return false;
    });
});