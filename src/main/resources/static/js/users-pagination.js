$(document).on("click", ".users-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#users-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});