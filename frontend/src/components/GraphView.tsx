import { useEffect, useRef, useState } from 'react'
import * as d3 from 'd3'

import { type GraphData, type GraphEdge, type GraphNode } from './GraphTypes'

type Props = {
  data: GraphData
  width?: number
  height?: number
  onNodeSelect?: (nodeId: string) => void
}

export function GraphView({ data, width = 800, height = 600, onNodeSelect }: Props) {
  const svgRef = useRef<SVGSVGElement | null>(null)
  const [size, setSize] = useState({ width, height })

  useEffect(() => {
    if (!svgRef.current) return

    const svgElement = svgRef.current
    const parent = svgElement.parentElement
    if (!parent) return

    const resizeObserver = new ResizeObserver((entries) => {
      const entry = entries[0]
      if (!entry) return

      setSize({
        width: Math.max(entry.contentRect.width, width),
        height: Math.max(entry.contentRect.height, height),
      })
    })

    resizeObserver.observe(parent)

    return () => resizeObserver.disconnect()
  }, [height, width])

  useEffect(() => {
    if (!svgRef.current) return

    const { width: canvasWidth, height: canvasHeight } = size
    const svg = d3.select(svgRef.current)
    svg.selectAll('*').remove()

    const graphLayer = svg.append('g').attr('class', 'graph-layer')

    const simulation = d3
      .forceSimulation<GraphNode>(data.nodes)
      .force(
        'link',
        d3
          .forceLink<GraphNode, GraphEdge>(data.edges)
          .id((d) => d.id)
          .distance((d) => {
            const source = typeof d.source === 'string' ? null : d.source
            const target = typeof d.target === 'string' ? null : d.target
            if (source?.type === 'topic' || target?.type === 'topic') return 150
            return 100
          })
          .strength((d) => {
            const source = typeof d.source === 'string' ? null : d.source
            const target = typeof d.target === 'string' ? null : d.target
            if (source?.type === 'topic' || target?.type === 'topic') return 0.95
            return 0.7
          }),
      )
      .force('charge', d3.forceManyBody().strength((d) => (d.type === 'topic' ? -900 : -350)))
      .force('center', d3.forceCenter(canvasWidth / 2, canvasHeight / 2))
      .force('collision', d3.forceCollide<GraphNode>().radius((d) => (d.type === 'topic' ? 44 : 22)).strength(0.9))

    const zoom = d3
      .zoom<SVGSVGElement, unknown>()
      .scaleExtent([0.85, 2])
      .translateExtent([
        [0, 0],
        [canvasWidth, canvasHeight],
      ])
      .on('zoom', (event) => {
        graphLayer.attr('transform', event.transform)
      })

    svg.call(zoom)

    const link = graphLayer
      .append('g')
      .attr('stroke', '#7c8698')
      .attr('stroke-opacity', 0.8)
      .selectAll('line')
      .data(data.edges)
      .join('line')
      .attr('stroke-width', (d) => Math.max((d.score ?? 0.35) * 3, 1.4))

    const node = graphLayer
      .append('g')
      .selectAll('circle')
      .data(data.nodes)
      .join('circle')
      .attr('r', (d) => (d.type === 'topic' ? 24 : 12))
      .attr('fill', (d) => {
        if (d.type === 'topic') return '#f59e0b'
        if (d.type === 'note') return '#2563eb'
        return '#10b981'
      })
      .attr('stroke', (d) => (d.type === 'topic' ? '#d97706' : '#ffffff'))
      .attr('stroke-width', (d) => (d.type === 'topic' ? 2.5 : 1.2))
      .style('cursor', 'pointer')
      .on('click', (_, nodeData) => {
        if (nodeData.type !== 'topic') {
          onNodeSelect?.(nodeData.id)
        }
      })
      .call(
        d3
          .drag<SVGCircleElement, GraphNode>()
          .on('start', dragStarted)
          .on('drag', dragged)
          .on('end', dragEnded),
      )

    const label = graphLayer
      .append('g')
      .selectAll('text')
      .data(data.nodes)
      .join('text')
      .text((d) => d.label)
      .attr('font-size', (d) => (d.type === 'topic' ? 12 : 10))
      .attr('font-weight', (d) => (d.type === 'topic' ? 700 : 400))
      .attr('fill', '#0f172a')
      .attr('text-anchor', (d) => (d.type === 'topic' ? 'middle' : 'start'))
      .attr('dx', (d) => (d.type === 'topic' ? 0 : 14))
      .attr('dy', (d) => (d.type === 'topic' ? 4 : 4))
      .style('cursor', 'pointer')
      .on('click', (_, nodeData) => {
        if (nodeData.type !== 'topic') {
          onNodeSelect?.(nodeData.id)
        }
      })

    simulation.on('tick', () => {
      link
        .attr('x1', (d) => (d.source as GraphNode).x!)
        .attr('y1', (d) => (d.source as GraphNode).y!)
        .attr('x2', (d) => (d.target as GraphNode).x!)
        .attr('y2', (d) => (d.target as GraphNode).y!)

      node
        .attr('cx', (d) => d.x!)
        .attr('cy', (d) => d.y!)

      label
        .attr('x', (d) => d.x!)
        .attr('y', (d) => d.y!)
    })

    function dragStarted(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0.3).restart()
      d.fx = d.x
      d.fy = d.y
    }

    function dragged(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      d.fx = event.x
      d.fy = event.y
    }

    function dragEnded(event: d3.D3DragEvent<SVGCircleElement, GraphNode, GraphNode>, d: GraphNode) {
      if (!event.active) simulation.alphaTarget(0)
      d.fx = null
      d.fy = null
    }

    return () => {
      simulation.stop()
    }
  }, [data, onNodeSelect, size])

  return <svg ref={svgRef} width={size.width} height={size.height} className="h-full w-full" />
}
