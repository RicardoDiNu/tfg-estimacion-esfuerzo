$(document).on("click", ".requirement-data-functions-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#requirement-data-functions-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});

$(document).on("click", ".requirement-transactional-functions-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#requirement-transactional-functions-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});