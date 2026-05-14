$(document).on("click", ".ucp-actors-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#actors-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});

$(document).on("click", ".ucp-modules-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#ucp-modules-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});

$(document).on("click", ".ucp-use-cases-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#use-cases-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});