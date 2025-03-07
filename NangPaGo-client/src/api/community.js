import axiosInstance from './axiosInstance';

export const createCommunity = async (data, file) => {
  const formData = new FormData();
  formData.append('title', data.title);
  formData.append('content', data.content);
  formData.append('isPublic', data.isPublic);
  if (file) formData.append('file', file);

  try {
    const response = await axiosInstance.post('/api/community', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
    });
    return response.data;
  } catch (error) {
    if (error.response?.data?.message) {
      throw new Error(`${error.response.data.message}`);
    }

    throw new Error(
      `커뮤니티를 생성하는 중 오류가 발생했습니다: ${error.message}`,
    );
  }
};

export const updateCommunity = async (id, data) => {
  try {
    const response = await axiosInstance.put(`/api/community/${id}`, data);
    return response.data;
  } catch (error) {
    if (error.response?.data?.message) {
      throw new Error(`${error.response.data.message}`);
    }
    throw new Error(
      `커뮤니티를 수정하는 중 오류가 발생했습니다: ${error.message}`,
    );
  }
};

export const deleteCommunity = async (id) => {
  try {
    const response = await axiosInstance.delete(`/api/community/${id}`);
    return response.data;
  } catch (error) {
    throw new Error(
      `커뮤니티를 삭제하는 중 오류가 발생했습니다: ${error.message}`,
    );
  }
};
