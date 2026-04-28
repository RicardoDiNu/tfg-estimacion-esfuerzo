$(document).on("click", ".requirements-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#requirements-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});

$(document).on("click", ".data-functions-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#data-functions-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});

$(document).on("click", ".transactional-functions-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#transactional-functions-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});


$(document).on("click", ".modules-page-link", function (e) {
    e.preventDefault();

    const ajaxUrl = $(this).attr("href");
    const browserUrl = $(this).data("browser-url");

    $.get(ajaxUrl, function (html) {
        $("#modules-container").replaceWith(html);

        if (browserUrl) {
            history.replaceState(null, "", browserUrl);
        }
    });
});

