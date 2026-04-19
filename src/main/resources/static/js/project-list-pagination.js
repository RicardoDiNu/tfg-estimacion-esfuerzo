$(document).on("click", ".projects-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#projects-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});