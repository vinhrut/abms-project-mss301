export function validateMaintenanceForm(formData) {
  const errors = {}

  if (!formData.category) {
    errors.category = 'Vui lòng chọn hạng mục.'
  }

  if (!formData.priority) {
    errors.priority = 'Vui lòng chọn mức ưu tiên.'
  }

  if (!formData.title?.trim()) {
    errors.title = 'Tiêu đề là bắt buộc.'
  } else if (formData.title.trim().length > 200) {
    errors.title = 'Tiêu đề tối đa 200 ký tự.'
  }

  if (formData.description && formData.description.length > 2000) {
    errors.description = 'Mô tả tối đa 2000 ký tự.'
  }

  return errors
}
