import { message } from "ant-design-vue";

// API 响应类型定义，匹配后端 ApiResponse 结构
export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

// 请求配置选项
export interface RequestOptions extends RequestInit {
  params?: Record<string, string | number | boolean | null | undefined>;
}

// API 基础路径（开发环境走 Vite 代理，生产环境由 nginx 转发）
export const BASE_URL = "/api";

/**
 * 构建完整的 URL（包含查询参数）
 */
function buildUrl(url: string, params?: Record<string, string | number | boolean | null | undefined>): string {
  const fullUrl = `${BASE_URL}${url}`;
  
  if (!params || Object.keys(params).length === 0) {
    return fullUrl;
  }

  const searchParams = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== null && value !== undefined) {
      searchParams.append(key, String(value));
    }
  });

  const queryString = searchParams.toString();
  return queryString ? `${fullUrl}?${queryString}` : fullUrl;
}

/**
 * 处理响应
 */
async function handleResponse<T>(response: Response): Promise<ApiResponse<T>> {
  if (!response.ok) {
    // HTTP 状态码错误
    const errorMsg = `HTTP ${response.status}`;
    message.error(errorMsg);
    throw new Error(errorMsg);
  }

  const data: ApiResponse<T> = await response.json();

  // 检查业务状态码
  if (data.code !== 200) {
    const errorMsg = data.message || "请求失败";
    message.error(errorMsg);
    throw new Error(errorMsg);
  }

  return data;
}

/**
 * 封装的 fetch 请求函数
 */
async function request<T = unknown>(
  url: string,
  options: RequestOptions = {}
): Promise<T> {
  const { params, headers, ...restOptions } = options;

  // 构建完整 URL
  const fullUrl = buildUrl(url, params);

  // 设置默认请求头（FormData 由浏览器自动设置 Content-Type 含 boundary）
  const isFormData = restOptions.body instanceof FormData;
  const effectiveHeaders: HeadersInit | undefined = isFormData
    ? (headers ? { ...headers } : undefined)
    : { "Content-Type": "application/json", ...headers };

  try {
    const response = await fetch(fullUrl, {
      ...restOptions,
      cache: "no-cache",
      ...(effectiveHeaders ? { headers: effectiveHeaders } : {}),
    });

    const apiResponse = await handleResponse<T>(response);
    return apiResponse.data;
  } catch (error) {
    // 统一错误处理
    if (error instanceof Error) {
      throw error;
    }
    throw new Error("网络请求失败");
  }
}

/**
 * GET 请求
 */
export function get<T = unknown>(
  url: string,
  params?: Record<string, string | number | boolean | null | undefined>,
  options?: Omit<RequestOptions, "method" | "body" | "params">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "GET",
    params,
  });
}

/**
 * POST 请求
 */
export function post<T = unknown>(
  url: string,
  data?: unknown,
  options?: Omit<RequestOptions, "method" | "body">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "POST",
    body: data instanceof FormData ? data : (data ? JSON.stringify(data) : undefined),
  });
}

/**
 * PUT 请求
 */
export function put<T = unknown>(
  url: string,
  data?: unknown,
  options?: Omit<RequestOptions, "method" | "body">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "PUT",
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * PATCH 请求
 */
export function patch<T = unknown>(
  url: string,
  data?: unknown,
  options?: Omit<RequestOptions, "method" | "body">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "PATCH",
    body: data ? JSON.stringify(data) : undefined,
  });
}

/**
 * DELETE 请求
 */
export function del<T = unknown>(
  url: string,
  params?: Record<string, string | number | boolean | null | undefined>,
  options?: Omit<RequestOptions, "method" | "body" | "params">
): Promise<T> {
  return request<T>(url, {
    ...options,
    method: "DELETE",
    params,
  });
}

// 导出默认对象，方便使用
export default {
  get,
  post,
  put,
  patch,
  delete: del,
};
