import Swal from 'sweetalert2';

function showSweeetChoice(message, options = {}) {
    const {
        title = 'Notification',
        icon = 'warning',  // The ‘warning； icon is used by default.
        confirmButtonText = 'OK',
        confirmButtonColor = '#3085d6',
        cancelButtonText = 'Cancel',
        cancelButtonColor = '#d33',
    } = options;

    return Swal.fire({
        title: title,
        text: message,
        icon: icon,
        showCancelButton: true,
        confirmButtonText: confirmButtonText,
        confirmButtonColor: confirmButtonColor,
        cancelButtonText: cancelButtonText,
        cancelButtonColor: cancelButtonColor,
    });
}

function showSweetAlertWithRetVal(message, options = {}) {
    const {
        title = 'Notification',
        icon = 'warning',
        confirmButtonText = 'OK',
        confirmButtonColor = '#3085d6',
    } = options;

    return Swal.fire({
        title: title,
        text: message,
        icon: icon,
        confirmButtonText: confirmButtonText,
        confirmButtonColor: confirmButtonColor,
    });
}

// Default popup, allows passing in custom parameters.
function showSweetAlert(message, options = {}) {
    const {
        title = 'Notification',   // default title
        icon = 'success',         // default icon
        confirmButtonText = 'OK', // default confirm button text
        confirmButtonColor = '#3085d6',  // default confirm button color (blue)
    } = options;

    Swal.fire({
        title: title,
        text: message,
        icon: icon,
        confirmButtonText: confirmButtonText,
        confirmButtonColor: confirmButtonColor,
    });
}

function showSweetError(message) {
    showSweetAlert(message, {
        icon: 'error',
        title: 'Error',
        confirmButtonColor: '#d33',
    });
}

export {
    Swal,
    showSweeetChoice,
    showSweetAlert,
    showSweetError,
    showSweetAlertWithRetVal,
};