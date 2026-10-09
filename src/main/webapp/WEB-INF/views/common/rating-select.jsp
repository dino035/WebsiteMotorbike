<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<label><c:out value="${param.label}"/>:</label>
<select name="<c:out value='${param.field}'/>" required>
    <option value="" ${empty param.selected ? 'selected' : ''}>-- Chọn mức --</option>
    <option value="POOR" ${param.selected == 'POOR' ? 'selected' : ''}>Kém</option>
    <option value="FAIR" ${param.selected == 'FAIR' ? 'selected' : ''}>Trung bình</option>
    <option value="GOOD" ${param.selected == 'GOOD' ? 'selected' : ''}>Tốt</option>
    <option value="EXCELLENT" ${param.selected == 'EXCELLENT' ? 'selected' : ''}>Rất tốt</option>
</select><br>