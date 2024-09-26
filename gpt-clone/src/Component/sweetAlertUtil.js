import Swal from 'sweetalert2';

function showSweeetChoice(message, options = {}) {
  const {
    title = 'Notification',
    icon = 'warning',  // 警告图标默认使用 'warning'
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

// 默认弹窗，允许传入自定义参数
function showSweetAlert(message, options = {}) {
  const {
    title = 'Notification',   // 默认标题
    icon = 'success',         // 默认图标
    confirmButtonText = 'OK', // 默认确认按钮文字
    confirmButtonColor = '#3085d6',  // 默认确认按钮颜色 (蓝色)
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
  showSweeetChoice,
  showSweetAlert,
  showSweetError,
  showSweetAlertWithRetVal,
};